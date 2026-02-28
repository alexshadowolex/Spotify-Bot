import com.adamratzman.spotify.SpotifyException
import com.adamratzman.spotify.endpoints.pub.SearchApi
import com.adamratzman.spotify.models.SimpleArtist
import com.adamratzman.spotify.models.Track
import com.adamratzman.spotify.utils.Market
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.tkuenneth.nativeparameterstoreaccess.NativeParameterStoreAccess
import com.github.tkuenneth.nativeparameterstoreaccess.WindowsRegistry
import com.github.twitch4j.TwitchClient
import com.github.twitch4j.TwitchClientBuilder
import com.github.twitch4j.chat.TwitchChat
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.github.twitch4j.common.enums.CommandPermission
import com.github.twitch4j.helix.domain.InboundFollow
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent
import config.BotConfig
import config.BuildInfo
import config.SpotifyConfig
import config.TwitchBotConfig
import handler.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.json.Json
import ui.screens.isFollowerOnlyModeEnabled
import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.time.format.DateTimeFormatterBuilder
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JOptionPane
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

var wasAlexAlreadyPraised = false
const val ALEX_TWITCH_USER_NAME = "alexshadowolex"

// Setup Twitch Bot
/**
 * Sets up and initializes the Twitch bot client with chat, PubSub, and Helix support.
 *
 * Connects to the configured channel, listens for reward redemptions and messages,
 * and handles command cooldowns per user and globally.
 * Also sets up event handling for user messages and initializes the necessary handlers for song requests.
 *
 * @param requestedByQueueHandler the instance managing the "requested by" queue for song requests
 * @return a fully initialized [TwitchClient] instance connected to the channel
 */
fun setupTwitchBot(requestedByQueueHandler: RequestedByQueueHandler): TwitchClient {
    val oAuth2Credential = OAuth2Credential("twitch", TwitchBotConfig.chatAccountToken)

    val twitchClient = TwitchClientBuilder.builder()
        .withEnableHelix(true)
        .withEnableChat(true)
        .withEnablePubSub(true)
        .withChatAccount(oAuth2Credential)
        .build()

    twitchClient.chat.run {
        connect()
        joinChannel(TwitchBotConfig.channel)
        sendMessageToTwitchChatAndLogIt(this, "Bot running peepoArrive")
    }

    val nextAllowedCommandUsageInstantPerUser = mutableMapOf<Pair<Command, /* user: */ String>, Instant>()
    val nextAllowedCommandUsageInstantPerCommand = mutableMapOf<Command, Instant>()

    val channelID = try {
        twitchClient.helix.getUsers(
            TwitchBotConfig.chatAccountToken,
            null,
            listOf(TwitchBotConfig.channel)
        ).execute().users.first().id
    } catch (e: NoSuchElementException) {
        logger.error("An Error occurred with the channel name. Maybe the channel name is spelled wrong?")
        logger.error(e.stackTraceToString())
        showErrorMessageWindow(
            message = "Error with channel name. Check the value of \"channel\" in the twitchBotConfig.properties-file!",
            title = "Error with channel name"
        )
        exitProcess(-1)
    }

    TwitchBotConfig.chatAccountID = channelID

    twitchClient.pubSub.listenForChannelPointsRedemptionEvents(
        oAuth2Credential,
        channelID
    )

    val removeSongFromQueueHandler = RemoveSongFromQueueHandler()
    
    twitchClient.eventManager.onEvent(RewardRedeemedEvent::class.java) { rewardRedeemEvent ->
        rewardRedeemEventHandler(rewardRedeemEvent, twitchClient, requestedByQueueHandler)
    }

    twitchClient.eventManager.onEvent(ChannelMessageEvent::class.java) { messageEvent ->
        val message = messageEvent.message

        if(messageEvent.user.name == ALEX_TWITCH_USER_NAME && !wasAlexAlreadyPraised) {
            praiseAlex(twitchClient.chat)
        }

        if (!message.startsWith(TwitchBotConfig.commandPrefix)) {
            return@onEvent
        }

        val parts = message.substringAfter(TwitchBotConfig.commandPrefix).split(" ")

        val command = commands.find {
            parts.first().substringAfter(TwitchBotConfig.commandPrefix).lowercase() in it.names
        } ?: return@onEvent

        val userName = messageEvent.user.name
        val userId = messageEvent.user.id
        val chat = messageEvent.twitchChat

        logger.info(
            "User ${userName.addQuotationMarks()} tried using command ${command.names.first().addQuotationMarks()}" +
            " with arguments: ${parts.drop(1).joinToString()}"
        )

        if(isUserBlacklisted(userName, userId)) {
            sendMessageToTwitchChatAndLogIt(chat, "$userName ${TwitchBotConfig.blacklistMessage}")
            return@onEvent
        }

        val nextAllowedCommandUsageInstant = nextAllowedCommandUsageInstantPerCommand.getOrPut(command) {
            Clock.System.now()
        }

        val nextAllowedCommandUsageInstantForUser = nextAllowedCommandUsageInstantPerUser.getOrPut(
            command to userName
        ) {
            Clock.System.now()
        }
        if(
            (Clock.System.now() - nextAllowedCommandUsageInstant).isNegative() &&
            !isUserBroadcaster(userName)
        ) {
            val secondsUntilTimeoutOver = (nextAllowedCommandUsageInstant - Clock.System.now()).inWholeSeconds.seconds

            sendMessageToTwitchChatAndLogIt(
                chat,
                "Command is still on cooldown. Please try again in $secondsUntilTimeoutOver"
            )
            logger.info("Command is still on cooldown.")

            return@onEvent
        }

        if (
            (Clock.System.now() - nextAllowedCommandUsageInstantForUser).isNegative() &&
            !isUserBroadcaster(userName)
        ) {
            val secondsUntilTimeoutOver = (
                    nextAllowedCommandUsageInstantForUser - Clock.System.now()
            ).inWholeSeconds.seconds

            sendMessageToTwitchChatAndLogIt(
                twitchClient.chat,
                "You are still on cooldown. Please try again in $secondsUntilTimeoutOver"
            )
            logger.info("User $userName is still on cooldown.")

            return@onEvent
        }

        val commandHandlerScope = CommandHandlerScope(
            twitchClient = twitchClient,
            messageEvent = messageEvent,
            removeSongFromQueueHandler = removeSongFromQueueHandler,
            requestedByQueueHandler = requestedByQueueHandler
        )

        backgroundCoroutineScope.launch {
            command.handler(commandHandlerScope, parts.drop(1))

            val key = command to userName
            nextAllowedCommandUsageInstantPerUser[key] = Clock.System.now() + commandHandlerScope.addedUserCoolDown

            nextAllowedCommandUsageInstantPerCommand[command] = Clock.System.now() + commandHandlerScope.addedCommandCoolDown
        }
    }

    logger.info("Twitch client started.")
    return twitchClient
}


/**
 * Handles a channel points reward redemption event.
 *
 * Looks up the corresponding redeem configuration and executes its handler
 * asynchronously if the user is not blacklisted.
 * Logs warnings if misconfigurations
 * are detected.
 *
 * @param redeemEvent the [RewardRedeemedEvent] triggered by Twitch
 * @param twitchClient the [TwitchClient] instance handling the bot connection
 * @param requestedByQueueHandler the queue handler for requested songs
 */
fun rewardRedeemEventHandler(
    redeemEvent: RewardRedeemedEvent,
    twitchClient: TwitchClient,
    requestedByQueueHandler: RequestedByQueueHandler
) {
    val redeemId = redeemEvent.redemption.reward.id
    val redeemTitle = redeemEvent.redemption.reward.title

    val redeem = redeems.find { redeemId in it.id || redeemTitle in it.id }.also {
        if (it != null) {
            if(redeemTitle in it.id) {
                logger.warn("Redeem $redeemTitle. " +
                        "Please use following ID in the properties file instead of the name: " +
                        redeemId
                )
            }
        }
    } ?: return

    if(isUserBlacklisted(redeemEvent.redemption.user.displayName, redeemEvent.redemption.user.id)) {
        logger.info("User ${redeemEvent.redemption.user} is blacklisted. Aborting")
        return
    }

    val redeemHandlerScope = RedeemHandlerScope(
        twitchClient = twitchClient,
        redeemEvent = redeemEvent,
        requestedByQueueHandler = requestedByQueueHandler
    )

    backgroundCoroutineScope.launch {
        redeem.handler(redeemHandlerScope, redeemEvent.redemption.userInput ?: "")
    }
}

// Logging
private const val LOG_DIRECTORY = "logs"
/**
 * Sets up the logging system for the bot.
 *
 * Creates a new log file in the "logs" directory with a timestamped name,
 * redirects standard output to both console and file, and logs bot version information.
 * Displays an error message window and exits the application on failure.
 */
fun setupLogging() {
    try {
        Files.createDirectories(Paths.get(LOG_DIRECTORY))

        val logFileName = DateTimeFormatterBuilder()
            .appendInstant(0)
            .toFormatter()
            .format(Clock.System.now().toJavaInstant())
            .replace(':', '-')

        val logFile = Paths.get(LOG_DIRECTORY, "${logFileName}.log").toFile().also {
            if (!it.exists()) {
                it.createNewFile()
            }
        }

        System.setOut(PrintStream(MultiOutputStream(System.out, FileOutputStream(logFile))))

        logger.info("Log file ${logFile.name.addQuotationMarks()} has been created.")
        logger.info("Bot version: v${BuildInfo.version}")
    } catch (_: Exception) {
        showErrorMessageWindow(
            message = "Error while setting up logging in setupLogging.",
            title = "Error while setting up logging."
        )
        exitProcess(-1)
    }
}


// General functions
/**
 * Reads a property value from the given [Properties] object.
 *
 * If the property cannot be read because it is not existing and the flag `setPropertyIfNotExisting`
 * is set to true, the property will be created with an empty string.
 * If not, displays an error dialog and terminates the application.
 *
 * @param properties the [Properties] instance to read from
 * @param propertyName the key of the property
 * @param propertiesFileRelativePath the relative path of the properties file (used in error messages)
 * @param setPropertyIfNotExisting if true, the property is created with an empty value when it does not exist;
 * otherwise, the application logs an error and terminates.
 * @return the raw value of the property as a [String]
 */
fun getPropertyValue(
    properties: Properties, propertyName: String,
    propertiesFileRelativePath: String,
    setPropertyIfNotExisting: Boolean
): String {
    return try {
        properties.getProperty(propertyName)
    } catch (e: Exception) {
        if(setPropertyIfNotExisting) {
            val emptyString = ""
            properties.setProperty(propertyName, emptyString)
            logger.info("Created property $propertyName in file $propertiesFileRelativePath with empty value.")
            emptyString
        } else {
            logger.error(
                "Exception occurred while reading property $propertyName in file $propertiesFileRelativePath: ",
                e
            )
            showErrorMessageWindow(
                message = "Error while reading value of property ${propertyName.addQuotationMarks()} " +
                        "in file $propertiesFileRelativePath.\n" +
                        "Try running the latest version of UpdateProperties.jar " +
                        "or fix it manually by adding it to the mentioned file.",
                title = "Error while reading properties"
            )
            logger.error("test")
            exitProcess(-1)
        }
    }
}


/**
 * Displays an error message in a modal dialog window.
 *
 * @param message the content of the error message
 * @param title the title of the dialog window
 */
fun showErrorMessageWindow(message: String, title: String) {
    JOptionPane.showMessageDialog(
        null,
        "$message\nCheck logs for more information",
        title,
        JOptionPane.ERROR_MESSAGE
    )
}


/**
 * Displays an error dialog for invalid enum property values.
 *
 * Logs the exception and shows which values are allowed.
 *
 * @param propertyName the name of the property that caused the error
 * @param propertyFilePath the path to the properties file
 * @param exception the exception that occurred during parsing
 * @param enumClassValues the list of allowed enum string values
 */
fun displayEnumParsingErrorWindow(
    propertyName: String,
    propertyFilePath: String,
    exception: Exception,
    enumClassValues: List<String>
) {
    logger.error(
        "Exception occurred while reading property ${propertyName.addQuotationMarks()} in file " + "$propertyFilePath: ",
        exception
    )
    showErrorMessageWindow(
        message = "Error while reading value of property ${propertyName.addQuotationMarks()} in file $propertyFilePath\n" +
                "Following values are allowed: " +
                enumClassValues.joinToString(),
        title = "Invalid value of property"
    )
}


/**
 * Determines whether the current Windows OS is using dark mode for apps.
 *
 * Reads the relevant registry key and compares against the expected dark mode value.
 *
 * @return true if Windows is in dark mode, false otherwise
 */
fun isWindowsInDarkMode(): Boolean {
    val windowsRegistryPath = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize"
    val windowsRegistryLightThemeParameter = "AppsUseLightTheme"
    val darkModeRegistryHexValue = 0x0

    return if (NativeParameterStoreAccess.IS_WINDOWS) {
        WindowsRegistry.getWindowsRegistryEntry(
            windowsRegistryPath,
            windowsRegistryLightThemeParameter
        ) == darkModeRegistryHexValue
    } else {
        false
    }
}


/**
 * Checks whether a given user is the broadcaster specified in the configuration.
 *
 * @param userName the username to check
 * @return true if the user is the broadcaster, false otherwise
 */
fun isUserBroadcaster(userName: String): Boolean {
    return userName == TwitchBotConfig.channel
}


// Twitch Bot functions
/**
 * Checks whether a user is blacklisted from using commands.
 *
 * Logs a warning if a blacklisted username is used instead of an ID.
 *
 * @param userName the username of the user
 * @param userId the Twitch user ID
 * @return true if the user is blacklisted, false otherwise
 */
fun isUserBlacklisted(userName: String, userId: String): Boolean {
    if(userName in BotConfig.blacklistedUsers || userId in BotConfig.blacklistedUsers) {
        if(userId !in BotConfig.blacklistedUsers) {
            logger.warn(
                "Blacklisted user $userName tried using a command. " +
                "Please use following ID in the properties file instead of the name: $userId"
            )
        }
        return true
    }
    return false
}


/**
 * Sends a message to Twitch chat and logs it to the console/log file.
 *
 * @param chat the [TwitchChat] instance to send the message through
 * @param message the message content
 */
fun sendMessageToTwitchChatAndLogIt(chat: TwitchChat, message: String) {
    chat.sendMessage(TwitchBotConfig.channel, message)
    logger.info("Sent Twitch chat message: $message")
}


/**
 * Checks if a user is either the broadcaster or a member of a custom group.
 *
 * Comparison is case-insensitive for group membership.
 *
 * @param userName the name of the user to check
 * @param customGroup the list of usernames in the custom group
 * @return true if the user is the broadcaster or part of the custom group, false otherwise
 */
fun isUserPartOfCustomGroupOrBroadcaster(userName: String, customGroup: List<String>): Boolean {
    return userName == TwitchBotConfig.channel || customGroup.contains(userName.lowercase(Locale.getDefault()))
}


/**
 * Performs general sanity checks for a command, ignoring security levels.
 *
 * Checks whether the command is enabled and optionally verifies follower-only mode restrictions.
 * Sends feedback messages to the user via Twitch chat if checks fail.
 *
 * @param commandName the name of the command (used in logging)
 * @param isCommandEnabledFlag whether the command is enabled
 * @param userName the username executing the command
 * @param userID the user ID executing the command
 * @param twitchClient the [TwitchClient] instance
 * @return true if all sanity checks pass, false otherwise
 */
fun handleCommandSanityChecksWithoutSecurityLevel(
    commandName: String,
    isCommandEnabledFlag: Boolean,
    userName: String,
    userID: String,
    twitchClient: TwitchClient
): Boolean {
    if(!isCommandEnabledFlag) {
        logger.info("$commandName disabled. Aborting execution")
        return false
    }

    if(BotConfig.isFollowerOnlyModeEnabled) {
        val isUserFollowingLongEnoughOrBroadcaster =
            isUserFollowingLongEnough(userID, twitchClient) ?: true ||
            isUserBroadcaster(userName)

        if(!isUserFollowingLongEnoughOrBroadcaster) {
            sendMessageToTwitchChatAndLogIt(twitchClient.chat, "You are not following long enough to use commands.")
            return false
        }
    }

    return true
}


/**
 * Performs full command sanity checks including security level validation.
 *
 * Combines the general sanity checks with a user-specific security level check.
 * Provides logging and Twitch chat feedback if checks fail.
 *
 * @param commandName the name of the command (used in logging)
 * @param isCommandEnabledFlag whether the command is enabled
 * @param messageEvent the [ChannelMessageEvent] containing user info
 * @param twitchClient the [TwitchClient] instance
 * @param securityCheckFunction function to verify user permissions
 * @param securityLevel the current security level of the command
 * @return true if all checks pass, false otherwise
 */
fun handleCommandSanityChecksWithSecurityLevel(
    commandName: String,
    isCommandEnabledFlag: Boolean,
    messageEvent: ChannelMessageEvent,
    twitchClient: TwitchClient,
    securityCheckFunction: (permission: Set<CommandPermission>, userName: String) -> Boolean,
    securityLevel: CustomCommandPermissions
): Boolean {

    if(!handleCommandSanityChecksWithoutSecurityLevel(
        commandName,
        isCommandEnabledFlag,
        messageEvent.user.name,
        messageEvent.user.id,
        twitchClient
    )) {
        return false
    }

    if(!securityCheckFunction(messageEvent.permissions, messageEvent.user.name)) {
        logger.info(
            "User ${messageEvent.user.name} tried using $commandName but was not eligible. " +
            "Current security setting: $securityLevel"
        )

        sendMessageToTwitchChatAndLogIt(twitchClient.chat, "You are not eligible to use that command!")
        return false
    }

    return true
}


/**
 * Determines if a user is currently following the broadcaster's channel.
 *
 * @param userID the Twitch user ID to check
 * @param twitchClient the [TwitchClient] instance
 * @return true if the user is following, false if not, null on error
 */
fun isUserFollowingChannel(userID: String, twitchClient: TwitchClient): Boolean? {
    val followingUserInformation = getUserFollowingInformation(userID, twitchClient)
    if(followingUserInformation == null) {
        logger.error("Couldn't check is user $userID is following in isUserFollowingChannel.")
        return null
    }

    return followingUserInformation.isNotEmpty()
}


/**
 * Retrieves information about whether a user is following the broadcaster's channel.
 *
 * Returns a list containing at most one [InboundFollow] object, or null on error.
 *
 * @param userID the Twitch user ID
 * @param twitchClient the [TwitchClient] instance
 * @return a list of [InboundFollow] objects, or null if an error occurred
 */
fun getUserFollowingInformation(userID: String, twitchClient: TwitchClient): List<InboundFollow>? {
    val followingUser = try {
        twitchClient.helix.getChannelFollowers(
            TwitchBotConfig.chatAccountToken,
            TwitchBotConfig.chatAccountID,
            userID,
            null,
            null
        ).execute()
    } catch (e: Exception) {
        logger.error("Couldn't get channel followers. Maybe the token needs more access rights?")
        logger.error(e.stackTraceToString())
        isFollowerOnlyModeEnabled.value = false
        backgroundCoroutineScope.launch {
            showErrorMessageWindow(
                title = "Follower only mode not working!",
                message =
                    "Follower only mode is not working right now and got disabled. You need to get " +
                    "a new token with more access rights.\n" +
                    "To get the new token, do following steps:\n" +
                    "\t1) Open your browser and log into the twitch account that the bot is using.\n" +
                    "\t2) Open a new tab and go to twitchtokengenerator.com.\n" +
                    "\t3) Click on \"Custom Scope Token\".\n" +
                    "\t4) Go all the way down beyond the long list of scopes and click on \"Select All\".\n" +
                    "\t5) Click on the green button \"Generate Token!\".\n" +
                    "\t6) After the new site loaded, click on \"Authorize\".\n" +
                    "\t7) Scroll up to the section \"Generated Tokens\" and copy the access token.\n" +
                    "\t8) Open the file \"twitchToken.txt\" in the bot's folder \"data\\tokens\" and replace the whole " +
                            "content (old token) with the new token.\n" +
                    "\t9) Restart the bot manually (close and open again).\n\n" +
                    "Note: The new token has a lot more access rights (all of them) just so we don't need to do " +
                            "that again in the future."
            )
        }
        return null
    }

    if(followingUser.follows == null) {
        logger.error("Something went wrong when getting follow information of user $userID.")
        return null
    }

    return followingUser.follows
}


/**
 * Calculates how long a user has been following the broadcaster.
 *
 * Returns -1 second if the user is not following.
 *
 * @param userID the Twitch user ID
 * @param twitchClient the [TwitchClient] instance
 * @return the duration the user has been following, -1 second if not following, null on error
 */
fun getUserFollowDuration(userID: String, twitchClient: TwitchClient): Duration? {
    val followingUserInformation = getUserFollowingInformation(userID, twitchClient)
    if(followingUserInformation == null) {
        logger.error("Couldn't get user's $userID following duration in getUserFollowDuration.")
        return null
    }

    if(isUserFollowingChannel(userID, twitchClient) != true) {
        return (-1).seconds
    }

    return Clock.System.now() - followingUserInformation.first().followedAt.toKotlinInstant()
}


/**
 * Checks if a user has been following the broadcaster long enough to meet
 * the minimum follow duration requirement.
 *
 * @param userID the Twitch user ID
 * @param twitchClient the [TwitchClient] instance
 * @return true if the following duration meets or exceeds the minimum, false if not, null on error
 */
fun isUserFollowingLongEnough(userID: String, twitchClient: TwitchClient): Boolean? {
    val followingDuration = getUserFollowDuration(userID, twitchClient)

    if(followingDuration == null) {
        logger.error("Couldn't check if user $userID is following long enough in isUserFollowingLongEnough.")
        return null
    }

    return followingDuration >= TwitchBotConfig.minimumFollowingDurationMinutes
}


/**
 * Sends a congratulatory message to the chat the first time the bot detects
 * the creator "alexshadowolex" sending a message.
 *
 * Ensures the praise only occurs once per session.
 *
 * @param chat the [TwitchChat] instance to send the message
 */
fun praiseAlex(chat: TwitchChat) {
    wasAlexAlreadyPraised = true
    logger.info("Praising alex")
    listOf(
        "It is him! The creator, the all knowing, the myth, the legend $ALEX_TWITCH_USER_NAME !",
        "$ALEX_TWITCH_USER_NAME ? More like the coolest programmer on earth (scientifically proven)",
        "OMG is that the guy that built me?! YEAH IT'S HIM, $ALEX_TWITCH_USER_NAME !!",
        "All rise! $ALEX_TWITCH_USER_NAME has entered the building \uD83E\uDEE1",
        "Sound the alarms! The mastermind $ALEX_TWITCH_USER_NAME is here!",
        "Everyone be cool. $ALEX_TWITCH_USER_NAME is watching \uD83D\uDC40",
        "From code to glory—$ALEX_TWITCH_USER_NAME just showed up \uD83D\uDCBB\uD83D\uDC51",
        "He didn’t just walk in… he deployed himself. Welcome, $ALEX_TWITCH_USER_NAME!",
        "Legend says $ALEX_TWITCH_USER_NAME coded this chat while skydiving \u2601\uFE0F\uD83D\uDCBB",
        "Hear ye, hear ye! The noble $ALEX_TWITCH_USER_NAME, architect of code and chaos, hath arrived!",
        "It’s a bird! It’s a plane! No—it’s $ALEX_TWITCH_USER_NAME!",
        "Brace yourselves. The wizard of ones and zeroes—$ALEX_TWITCH_USER_NAME—is here!",
        "You hear that? That’s the sound of greatness logging in: $ALEX_TWITCH_USER_NAME \uD83C\uDFA7",
        "Bleep bloop. Creator identified: $ALEX_TWITCH_USER_NAME. Initiating admiration protocol.",
        "$ALEX_TWITCH_USER_NAME joined the chat. FPS just went up 20% from pure charisma.",
        "OH MY GOD CHAT!! IT’S HIM!! IT’S REALLY HIM!! $ALEX_TWITCH_USER_NAME IS HERE AAAAA \uD83D\uDE31",
        "The dev, the myth, the chillest coder alive: $ALEX_TWITCH_USER_NAME \uD83D\uDE4C"
    ).random().run{ sendMessageToTwitchChatAndLogIt(chat, this) }
}


// Spotify Functions
/**
 * Handles a song request query from Twitch chat.
 *
 * Attempts to add the song to the Spotify queue, providing feedback to the user
 * in Twitch chat.
 * Returns whether the song was successfully added.
 *
 * @param chat the [TwitchChat] instance to send messages
 * @param query the search query or Spotify link
 * @return true if the song was successfully queued, false otherwise
 */
suspend fun handleSongRequestQuery(chat: TwitchChat, query: String): Boolean {
    logger.info("called handleSongRequestQuery with query $query")
    var success = true
    try {
        val message = updateQueue(query).let { result ->
            val track = result.track
            if(track != null) {
                "Song ${createSongString(track.name, track.artists)} has been added to the queue " +
                TwitchBotConfig.songRequestEmotes.run {
                    if(this.isNotEmpty()) {
                        this.random()
                    } else {
                        ""
                    }
                }
            } else {
                success = false
                "Couldn't add song to the queue. ${result.songRequestResultExplanation}"
            }
        }
        sendMessageToTwitchChatAndLogIt(chat, message)

    } catch (e: Exception) {
        logger.error("Something went wrong in handleSongRequestQuery ", e)
        success = false
    }

    return success
}


/**
 * Adds a song to the Spotify playback queue based on a search query or direct link.
 *
 * Performs checks for blocked songs/artists and maximum length, logging
 * all relevant events. Returns the track and an explanation message.
 *
 * @param query a search string or Spotify track link
 * @return a [SongRequestResult] containing the track (or null) and a result explanation
 */
private suspend fun updateQueue(query: String): SongRequestResult {
    logger.info("called updateQueue with query $query")
    val result = try {
        getSongIdFromSpotifyDirectLink(query)?.let {
            getSpotifyTrackById(it)
        } ?: run {
            if(isUrlSpotifyDirectLink(Url(query))) {
                return SongRequestResult(
                    track = null,
                    songRequestResultExplanation = "Spotify link is not a link to a song."
                )
            }
            getSpotifyTrackByQuery(query)
        } ?: return SongRequestResult(
            track = null,
            songRequestResultExplanation = "No Result when searching for song."
        )
    } catch (e: Exception) {
        logger.error("Error while searching for track:", e)
        return SongRequestResult(
            track = null,
            songRequestResultExplanation = "Exception when accessing spotify endpoints for searching the song."
        )
    }

    logger.info("Result after accessing spotify endpoints: $result")
    val artistNames = result.artists.map { it.name }

    if(isSongBlocked(result.uri.id) || isSongArtistBlocked(artistNames)) {
        val message = "Song ${result.name.addQuotationMarks()} was blocked because of " +
            if(isSongArtistBlocked(artistNames)) {
                "the artist ${getFirstBlockedArtistName(artistNames).addQuotationMarks()} being blocked."
            } else {
                "the song itself being blocked."
            }
        logger.info(message)
        return SongRequestResult(
            track = null,
            songRequestResultExplanation = message
        )
    }

    if(result.length.milliseconds > SpotifyConfig.maximumLengthSongRequestMinutes) {
        logger.info("Song length ${result.length / 60000f} was longer than ${SpotifyConfig.maximumLengthSongRequestMinutes}")
        return SongRequestResult(
            track = null,
            songRequestResultExplanation = "The song was longer than ${SpotifyConfig.maximumLengthSongRequestMinutes}."
        )
    }

    try {
        spotifyClient.player.addItemToEndOfQueue(result.uri)
        logger.info("Added Song with URI ${result.uri.uri} successfully to the queue.")
    } catch (e: Exception) {
        val message = when(e) {
            is SpotifyException.BadRequestException -> {
                logger.error("Spotify player is not active.", e)
                "Spotify Player is currently not active. Click on the spotify play button strimmer!"
            }
            else -> {
                logger.error("An exception occurred while calling addItemToEndOfQueue: ", e)
                "Adding the song to the queue failed."
            }
        }
        return SongRequestResult(
            track = null,
            songRequestResultExplanation = message
        )
    }

    return SongRequestResult(
        track = result,
        songRequestResultExplanation = "Successfully added the song to the queue."
    )
}



/**
 * Extracts the Spotify track ID from a direct track URL.
 *
 * @param directLink a full Spotify track link
 * @return the track ID if valid, null otherwise
 */
fun getSongIdFromSpotifyDirectLink(directLink: String): String? {
    return Url(directLink).takeIf { isUrlSpotifyTrackDirectLink(it) }
        ?.encodedPath?.substringAfter("/track/")
}


/**
 * Returns the first blocked artist from a list of artist names according to configuration.
 *
 * Comparison is case-insensitive.
 * Returns an empty string if no artist is blocked.
 *
 * @param artists the list of artist names
 * @return the first blocked artist name or empty string if none
 */
private fun getFirstBlockedArtistName(artists: List<String?>): String {
    for(artist in artists.map { it?.lowercase(Locale.getDefault()) ?: "" }) {
        if(SpotifyConfig.blockedSongArtists.contains(artist)) {
            return artist
        }
    }
    return ""
}


/**
 * Determines whether a specific song is blocked based on its ID.
 *
 * @param songId the Spotify track ID
 * @return true if the song is blocked, false otherwise
 */
private fun isSongBlocked(songId: String): Boolean {
    return SpotifyConfig.blockedSongLinks.map { getSongIdFromSpotifyDirectLink(it) ?: "" }.contains(songId)
}


/**
 * Determines whether any artist in the given list is blocked.
 *
 * Comparison is case-insensitive.
 *
 * @param artists the list of artist names
 * @return true if at least one artist is blocked, false otherwise
 */
private fun isSongArtistBlocked(artists: List<String?>): Boolean {
    for(artist in artists.map { it?.lowercase(Locale.getDefault()) ?: "" }) {
        if(SpotifyConfig.blockedSongArtists.contains(artist)) {
            return true
        }
    }
    return false
}


/**
 * Determines whether the given URL is a direct Spotify link to a track.
 *
 * This validates both that the URL points to Spotify and that its path
 * explicitly references a track resource.
 *
 * @param url the URL to evaluate
 * @return true if the URL is a Spotify track link, false otherwise
 */
private fun isUrlSpotifyTrackDirectLink(url: Url): Boolean {
    return isUrlSpotifyDirectLink(url) && url.encodedPath.contains("/track/")
}


/**
 * Checks whether the given URL points to Spotify's web player domain.
 *
 * This function does not validate the resource type (track, album, playlist, etc.),
 * only that the URL originates from Spotify.
 *
 * @param url the URL to check
 * @return true if the URL belongs to Spotify, false otherwise
 */
private fun isUrlSpotifyDirectLink(url: Url): Boolean {
    return url.host == "open.spotify.com"
}


/**
 * Retrieves a Spotify track by its unique track ID using the Tracks API.
 *
 * Any API or network error is logged and results in a null return value.
 *
 * @param songId the Spotify track ID
 * @return the retrieved Track on success, or null if the request fails
 */
private suspend fun getSpotifyTrackById(songId: String): WorkaroundTrack? {
    logger.info("called getSpotifyTrackById with ID: $songId")
    return try {
        // TODO Remove when fixed in Spotify-Kotlin-API
        spotifyClientWorkaroundHandler.getTrack(songId)
        /*
        spotifyClient.tracks.getTrack(
            track = songId,
            market = Market.DE
        )
         */
    } catch (e: Exception) {
        logger.error("Exception while accessing tracks endpoint of spotify: ", e)
        null
    }
}


/**
 * Searches for a Spotify track using a free-text query.
 *
 * If the query is a Spotify direct link that does not point to a track,
 * the search is intentionally aborted to avoid unrelated results.
 *
 * @param query the search query string
 * @return the first matching Track on success, or null if none is found or an error occurs
 */
private suspend fun getSpotifyTrackByQuery(query: String): WorkaroundTrack? {
    logger.info("called getSpotifyTrackByQuery with query: $query")
    if(isUrlSpotifyDirectLink(Url(query))) {
        logger.info("Query is a spotify direct link to something but not to a track. Aborting the search")
        return null
    }

    return try {
        spotifyClientWorkaroundHandler.search(query)?.tracks?.firstOrNull()
        /*
        spotifyClient.search.search(
            query = query,
            searchTypes = arrayOf(
                SearchApi.SearchType.Artist,
                SearchApi.SearchType.Album,
                SearchApi.SearchType.Track
            ),
            market = Market.DE,
            limit = 1
        ).tracks?.firstOrNull()
         */
    } catch (e: Exception) {
        logger.error("Exception while accessing search endpoint of spotify: ", e)
        null
    }
}


/**
 * Retrieves the currently playing Spotify track for the active player.
 *
 * If no playback is active or an error occurs while querying the API,
 * null is returned.
 *
 * @return the currently playing Track, or null if unavailable
 */
suspend fun getCurrentSpotifySong(): Track? {
    return try {
        spotifyClient.player.getCurrentlyPlaying()?.item as Track
    } catch (_: Exception) {
        null
    }
}


/**
 * Builds a human-readable string representation of a song.
 *
 * The resulting format is:
 * `"Song Title" by Artist 1, Artist 2 and Artist 3`
 *
 * @param name the song title
 * @param artists the list of contributing artists
 * @return a formatted song description string
 */
fun createSongString(name: String, artists: List<SimpleArtist>): String {
    return "${name.addQuotationMarks()} by ${getArtistsString(artists)}"
}


/**
 * Formats a list of artists into a natural-language string.
 *
 * Artists are separated by commas, with the final two joined using "and".
 *
 * @param artists the list of artists
 * @return a readable concatenation of artist names
 */
fun getArtistsString(artists: List<SimpleArtist>): String {
    return artists.map { it.name }.let { artist ->
        listOf(
            artist.dropLast(1).joinToString(),
            artist.last()
        ).filter { it!!.isNotBlank() }.joinToString(" and ")
    }
}


private const val CURRENT_SONG_FILE_NAME = "currentSong.txt"
private const val CURRENT_SONG_NAME_FILE_NAME = "currentSongName.txt"
private const val CURRENT_SONG_ARTISTS_FILE_NAME = "currentSongArtists.txt"
private const val CURRENT_SONG_ALBUM_IMAGE_FILE_NAME = "currentAlbumImage.jpg"
private const val CURRENT_REQUESTED_BY_FILE_NAME = "currentRequestedByText.txt"
private const val DISPLAY_FILES_DIRECTORY = "data\\displayFiles"
/**
 * Starts a background coroutine that continuously fetches and persists
 * the currently playing Spotify song.
 *
 * On startup, required directories and files are created if missing.
 * While enabled, the current song information is refreshed every two seconds
 * and written to display files used by external systems (e.g., stream overlays).
 *
 * @param requestedByQueueHandler handler used to resolve and update request metadata
 */
fun startSpotifySongGetter(requestedByQueueHandler: RequestedByQueueHandler) {
    logger.info("called startSpotifySongGetter")
    backgroundCoroutineScope.launch {
        createSongDisplayFolderAndFiles()

        while(isActive) {
            if(!isSpotifySongNameGetterEnabled()) {
                emptyAllSongDisplayFiles()
                delay(0.5.seconds)
                continue
            }

            val isPlaying = if(BotConfig.isEmptySongDisplayFilesOnPauseEnabled) {
                isSpotifyPlaying()
            } else {
                true
            }

            if(isPlaying == true || isPlaying == null) {
                val currentTrack = getCurrentSpotifySong()
                if (currentTrack == null) {
                    delay(1.seconds)
                    continue
                }

                val currentSpotifySongBefore = currentSpotifySong
                currentSpotifySong = currentTrack

                if(currentTrack != currentSpotifySongBefore) {
                    // Has to be called after the currentSpotifySong got updated to correctly update the values
                    requestedByQueueHandler.updateRequestedByQueue(currentSpotifySongBefore)
                }
                // Has to be assigned after the update of the requested by queue
                val currentRequestedByUsername = requestedByQueueHandler.currentRequestedByUsername

                downloadAndSaveAlbumImage(currentTrack)
                writeCurrentSongTextFiles(currentTrack, currentRequestedByUsername)
            } else {
                emptyAllSongDisplayFiles()
            }

            delay(2.seconds)
        }
    }
}


/**
 * Safely checks whether the Spotify song name getter feature is enabled.
 *
 * Any exception while accessing configuration values is treated as disabled.
 *
 * @return true if enabled, false otherwise
 */
fun isSpotifySongNameGetterEnabled(): Boolean {
    return try {
        BotConfig.isSpotifySongNameGetterEnabled
    } catch (_: Exception) {
        false
    }
}


/**
 * Writes the current song metadata to display text files.
 *
 * If the song was requested by a user, an additional file is populated
 * indicating the requesting username.
 *
 * @param currentTrack the currently playing Spotify track
 * @param currentRequestedByUsername the username that requested the song, or null
 */
private fun writeCurrentSongTextFiles(currentTrack: Track, currentRequestedByUsername: String?) {
    try {
        val currentSongInputString = createSongString(currentTrack.name, currentTrack.artists)
        val currentRequestedByString = if(currentRequestedByUsername != null) {
            "requested by $currentRequestedByUsername"
        } else {
            ""
        }

        File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_FILE_NAME")
            .writeText(currentSongInputString + " ".repeat(10))
        File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_NAME_FILE_NAME")
            .writeText(currentTrack.name)
        File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_ARTISTS_FILE_NAME")
            .writeText(getArtistsString(currentTrack.artists))
        File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_REQUESTED_BY_FILE_NAME")
            .writeText(currentRequestedByString)
    } catch (e: Exception) {
        logger.error("Exception occurred while trying to save the song in files in writeCurrentSongTextFiles ", e)
    }
}


/**
 * Downloads and stores the album artwork of the current track.
 *
 * If the image resolution differs from the default 640x640 pixels,
 * it is resized before being saved.
 *
 * @param currentTrack the currently playing Spotify track
 */
private fun downloadAndSaveAlbumImage(currentTrack: Track) {
    try {
        val images = currentTrack.album.images
        if (!images.isNullOrEmpty()) {
            val imageUrl = images.first().url
            val url = URL(imageUrl)
            val bufferedImageData = ImageIO.read(url)

            val defaultWidth = 640
            val defaultHeight = 640
            val isImageWidthWrong = bufferedImageData.width != defaultWidth
            val isImageHeightWrong = bufferedImageData.height != defaultHeight

            val imageBytes = if(isImageWidthWrong || isImageHeightWrong) {
                val resizedImage = bufferedImageData.getScaledInstance(defaultWidth, defaultHeight, Image.SCALE_DEFAULT)
                val newImageBuffer = BufferedImage(defaultWidth, defaultHeight, BufferedImage.TYPE_INT_RGB)
                newImageBuffer.graphics.drawImage(resizedImage, 0, 0, Color(0, 0, 0), null)

                val byteArrayOutputStream = ByteArrayOutputStream()
                ImageIO.write(newImageBuffer, "jpg", byteArrayOutputStream)

                byteArrayOutputStream.toByteArray()
            } else {
                url.readBytes()
            }

            File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_ALBUM_IMAGE_FILE_NAME").writeBytes(imageBytes)
        }
    } catch (e: Exception) {
        logger.error("Exception occurred while trying to get the image in downloadAndSaveAlbumImage ", e)
    }
}


/**
 * Ensures the song display directory and required files exist.
 *
 * Missing folders and files are created asynchronously on startup.
 */
private fun createSongDisplayFolderAndFiles() {
    backgroundCoroutineScope.launch {
        val displayFilesDirectory = File(DISPLAY_FILES_DIRECTORY)
        if (!displayFilesDirectory.exists() || !displayFilesDirectory.isDirectory) {
            displayFilesDirectory.mkdirs()
            logger.info("Created display file folder $DISPLAY_FILES_DIRECTORY")
        }

        listOf(
            File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_FILE_NAME"),
            File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_NAME_FILE_NAME"),
            File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_ARTISTS_FILE_NAME"),
            File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_ALBUM_IMAGE_FILE_NAME"),
            File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_REQUESTED_BY_FILE_NAME")
        ).forEach { currentFile ->
            if (!currentFile.exists()) {
                withContext(Dispatchers.IO) {
                    try {
                        currentFile.createNewFile()
                        logger.info("Created current song display file ${currentFile.name}")
                    } catch (_: Exception) {
                        logger.error("Error while creating song display file ${currentFile.name} in createSongDisplayFolderAndFiles")
                    }
                }
            }
        }
    }
}


/**
 * Clears all song display files.
 *
 * Text files are emptied, and the album image file is replaced
 * with a minimal placeholder image.
 */
fun emptyAllSongDisplayFiles() {
    listOf(
        File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_FILE_NAME"),
        File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_NAME_FILE_NAME"),
        File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_ARTISTS_FILE_NAME"),
        File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_REQUESTED_BY_FILE_NAME")
    ).forEach{ currentFile ->
        currentFile.writeText("")
    }

    File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_ALBUM_IMAGE_FILE_NAME").writeBytes(
        try {
            object {}.javaClass.getResourceAsStream("Blank.jpg")!!.readAllBytes()
        } catch (_: Exception) {
            logger.error("Error while reading \"Blank.jpg\" data in emptyAllSongDisplayFiles")
            byteArrayOf()
        }
    )
}


/**
 * Determines whether song requests are enabled exclusively as channel redeems.
 *
 * @return true if song requests are enabled as redeems, false otherwise
 */
fun isSongRequestEnabledAsRedeem(): Boolean {
    return !BotConfig.isSongRequestCommandEnabled
}


/**
 * Checks whether song requests are enabled and handled via chat commands.
 *
 * @return true if song request commands are active, false otherwise
 */
fun isSongRequestCommandActive(): Boolean {
    return BotConfig.isSongRequestCommandEnabled && BotConfig.isSongRequestEnabled
}


/**
 * Checks whether song requests are enabled and handled via channel redeems.
 *
 * @return true if song request redeems are active, false otherwise
 */
fun isSongRequestRedeemActive(): Boolean {
    return isSongRequestEnabledAsRedeem() && BotConfig.isSongRequestEnabled
}


/**
 * Queries the Spotify Player API to determine playback state, see
 * https://developer.spotify.com/documentation/web-api/reference/get-information-about-the-users-current-playback
 *
 * @return true if playback is active, false if paused or inactive, or null on error
 */
suspend fun isSpotifyPlaying(): Boolean? {
    val playbackEndpoint = "https://api.spotify.com/v1/me/player"
    val json = Json { ignoreUnknownKeys = true }

    val response = try {
        httpClient.get(playbackEndpoint) {
            header("Authorization", "Bearer ${spotifyClient.token.accessToken}")
        }
    } catch (e: Exception) {
        logger.error("An error occurred while sending request to player endpoint: ", e)
        return null
    }

    val isPlaying = when (response.status) {
        HttpStatusCode.OK -> {
            try {
                json.decodeFromString<SimplifiedSpotifyPlaybackResponse>(response.bodyAsText()).is_playing
            } catch (e: Exception) {
                logger.error("Exception while parsing Spotify Playback response: ", e)
                null
            }
        }
        // Player not active
        HttpStatusCode.NoContent -> {
            false
        }
        // Error cases (401, 403, 429)
        else -> {
            logger.error("An error occurred while accessing player endpoint. Code: ${response.status}. " +
                    "Message: ${response.bodyAsText()}"
            )
            null
        }
    }

    return isPlaying
}


/**
 * Adds the given track to the specified Spotify playlist.
 *
 * @param song the track to add
 * @param playlistId the target playlist ID
 * @return true if the operation succeeded, false otherwise
 */
suspend fun addSongToPlaylist(song: Track, playlistId: String): Boolean {
    logger.info("called addSongToPlaylist")
    var success = true
    try {
        // TODO Remove when fixed in Spotify-Kotlin-API
        success = spotifyClientWorkaroundHandler.addItemsToPlaylist(playlistId, song.uri)
        /*spotifyClient.playlists.addPlayableToClientPlaylist(
            playlistId,
            song.uri
        )*/
    } catch (e: SpotifyException.BadRequestException) {
        logger.error("Something went wrong when adding song to the playlist in addSongToPlaylist: ", e)
        success = false
    }

    return success
}


/**
 * Checks whether a user is allowed to use the add-song command.
 *
 * @param permissions the user's permission set
 * @param userName the user's name
 * @return true if the user is eligible, false otherwise
 */
fun isUserEligibleForAddSongCommand(permissions: Set<CommandPermission>, userName: String): Boolean {
    logger.info("called isUserEligibleForAddSongCommand")
    return isUserEligibleForCommand(
        permissions,
        userName,
        BotConfig.addSongCommandSecurityLevel,
        BotConfig.customGroupUserNamesAddSongCommand
    )
}


/**
 * Retrieves the name of a Spotify playlist by its ID.
 *
 * @param playlistId the playlist ID
 * @return the playlist name, or an empty string if retrieval fails
 */
suspend fun getPlaylistName(playlistId: String): String {
    return try {
        spotifyClient.playlists.getPlaylist(playlistId)?.name ?: ""
    } catch (e: Exception) {
        logger.error("Caught error while getting name of playlist in getPlaylistName: ", e)
        ""
    }
}


/**
 * Validates whether the given Spotify playlist ID exists.
 *
 * @param playlistId the playlist ID to validate
 * @return true if valid, false otherwise
 */
suspend fun isPlaylistIdValid(playlistId: String): Boolean {
    val result = try {
        spotifyClient.playlists.getPlaylist(playlistId) ?: return false
    } catch (e: Exception) {
        logger.error("Caught error while checking if playlist ID is valid in isPlaylistIdValid: ", e)
        return false
    }
    return result.id == playlistId
}


/**
 * Checks whether a track is already contained in a Spotify playlist.
 *
 * @param songId the track ID to check
 * @param playlistId the playlist ID
 * @return true if present, false if not, or null on error
 */
suspend fun isSongInPlaylist(songId: String, playlistId: String): Boolean? {
    val playlistSongIds = getPlaylistSongIds(playlistId) ?: return null
    return playlistSongIds.contains(songId)
}


/**
 * Retrieves all track IDs from a Spotify playlist.
 *
 * The playlist is fetched in pages of up to 50 tracks until complete.
 *
 * @param playlistId the playlist ID
 * @return a list of track IDs, or null if retrieval fails
 */
suspend fun getPlaylistSongIds(playlistId: String): List<String?>? {
    val playlistSongIds = mutableListOf<String?>()
    val limit = 50
    var currentOffset = 0
    var nextLink: String? = ""

    while(nextLink != null) {
        val result = try {
            // TODO Remove when fixed in Spotify-Kotlin-API
            spotifyClientWorkaroundHandler.getPlaylistItems(playlistId, currentOffset, limit)

            /*
            spotifyClient.playlists.getPlaylistTracks(
                playlist = playlistId,
                offset = currentOffset,
                limit = limit
            )
            */
        } catch (e: Exception) {
            logger.error("Error while getting playlistTracks in getPlaylistSongIds with playlistId $playlistId: ", e)
            null
        } ?: return null

        nextLink = result.next
        currentOffset += limit

        playlistSongIds.addAll(result.items.map { it.track?.id ?: "" })
    }

    return playlistSongIds
}


/**
 * Executes the add-song command logic after validation.
 *
 * The track is added only if it does not already exist in the target playlist.
 *
 * @param song the track to add
 * @return a user-facing status message
 */
suspend fun handleAddSongCommandFunctionality(song: Track): String {
    return when(isSongInPlaylist(song.id, SpotifyConfig.playlistIdForAddSongCommand)) {
        true -> {
            "Song ${song.name.addQuotationMarks()} is already in playlist ${getAddSongPlaylistNameString()}"
        }

        false -> {
            val success = addSongToPlaylist(song, SpotifyConfig.playlistIdForAddSongCommand)
            if(success) {
                "Successfully added song ${song.name.addQuotationMarks()} to the playlist ${getAddSongPlaylistNameString()}"
            } else {
                "Error while adding song to the playlist!"
            }
        }

        null -> {
            "Error while adding song to the playlist. Check the playlist ID"
        }
    }
}


/**
 * Retrieves and caches the display name of the add-song playlist.
 *
 * If not yet cached, the playlist name is fetched from Spotify.
 *
 * @return the playlist name wrapped in quotation marks
 */
suspend fun getAddSongPlaylistNameString(): String {
    if(SpotifyConfig.playlistNameForAddSongCommand.isEmpty()) {
        SpotifyConfig.playlistNameForAddSongCommand = getPlaylistName(SpotifyConfig.playlistIdForAddSongCommand)
    }

    return SpotifyConfig.playlistNameForAddSongCommand.addQuotationMarks()
}


/**
 * Checks whether a user is allowed to use the skip-song command.
 *
 * @param permissions the user's permission set
 * @param userName the user's name
 * @return true if the user is eligible, false otherwise
 */
fun isUserEligibleForSkipSongCommand(permissions: Set<CommandPermission>, userName: String): Boolean {
    logger.info("called isUserEligibleForSkipSongCommand")
    return isUserEligibleForCommand(
        permissions,
        userName,
        BotConfig.skipSongCommandSecurityLevel,
        BotConfig.customGroupUserNamesSkipSongCommand
    )
}


/**
 * Checks whether a user is allowed to remove a song from the queue.
 *
 * @param permissions the user's permission set
 * @param userName the user's name
 * @return true if the user is eligible, false otherwise
 */
fun isUserEligibleForRemoveSongFromQueueCommand(permissions: Set<CommandPermission>, userName: String): Boolean {
    logger.info("called isUserEligibleForRemoveSongFromQueueCommand")
    return isUserEligibleForCommand(
        permissions,
        userName,
        BotConfig.removeSongFromQueueCommandSecurityLevel,
        BotConfig.customGroupUserNamesRemoveSongFromQueueCommand
    )
}


/**
 * Checks whether a user is allowed to pause or resume playback.
 *
 * @param permissions the user's permission set
 * @param userName the user's name
 * @return true if the user is eligible, false otherwise
 */
fun isUserEligibleForPauseResumeCommand(permissions: Set<CommandPermission>, userName: String): Boolean {
    logger.info("called isUserEligibleForPauseResumeCommand")
    return isUserEligibleForCommand(
        permissions,
        userName,
        BotConfig.pauseResumeCommandSecurityLevel,
        BotConfig.customGroupUserNamesPauseResumeCommand
    )
}


/**
 * Checks whether a user is allowed to block a song.
 *
 * @param permissions the user's permission set
 * @param userName the user's name
 * @return true if the user is eligible, false otherwise
 */
fun isUserEligibleForBlockSongCommand(permissions: Set<CommandPermission>, userName: String): Boolean {
    logger.info("called isUserEligibleForBlockSongCommand")
    return isUserEligibleForCommand(
        permissions,
        userName,
        BotConfig.blockSongCommandSecurityLevel,
        BotConfig.customGroupUserNamesBlockSongCommand
    )
}


/**
 * Evaluates user eligibility based on a command's configured security level.
 *
 * Custom security checks validate broadcaster or custom-group membership,
 * while predefined levels rely on permission flags.
 *
 * @param permissions the user's permission set
 * @param userName the user's name
 * @param commandSecurityLevel the command's security configuration
 * @param customGroup list of custom-authorized usernames
 * @return true if the user is eligible, false otherwise
 */
fun isUserEligibleForCommand(
    permissions: Set<CommandPermission>,
    userName: String,
    commandSecurityLevel: CustomCommandPermissions,
    customGroup: List<String>
): Boolean {
    return if(commandSecurityLevel == CustomCommandPermissions.CUSTOM) {
        isUserPartOfCustomGroupOrBroadcaster(userName, customGroup)
    } else {
        permissions.contains(CommandPermission.valueOf(commandSecurityLevel.toString()))
    }
}


/**
 * Retrieves the Spotify device ID currently associated with playback.
 *
 * This function first attempts to resolve the device ID from the current playback
 * context.
 * If no active context is available, it falls back to the first device
 * returned by the user's available Spotify devices list.
 *
 * If an exception occurs (for example, when no devices are available or the API
 * request fails), the error is logged and `null` is returned.
 *
 * @return the resolved Spotify device ID, or `null` if no device could be found
 */
suspend fun getCurrentDeviceId(): String? {
    return try {
        spotifyClient.player.getCurrentContext()?.device?.id ?:
            spotifyClient.player.getDevices().first().id
    } catch (_: Exception) {
        logger.warn("No device found in getCurrentDeviceId")
        null
    }
}


// Github
const val GITHUB_LATEST_VERSION_LINK = "https://github.com/alexshadowolex/Spotify-Bot/releases/latest"


/**
 * Fetches and persists information about the latest GitHub release.
 *
 * Stored data includes the latest version number, release notes,
 * and available release assets.
 */
suspend fun saveLatestGitHubReleaseInformation() {
    val textBeforeVersionNumber = "v"
    val gitHubOwner = "alexshadowolex"
    val giHubRepo = "Spotify-Bot"
    val latestReleaseEndpoint = "https://api.github.com/repos/$gitHubOwner/$giHubRepo/releases/latest"

    val json = Json { ignoreUnknownKeys = true }

    val response = try {
        httpClient.get(latestReleaseEndpoint) {
            header("ACCEPT", "application/vnd.github+json")
        }
    } catch (e: Exception) {
        logger.error("Error while getting information from GitHub.")
        logger.error(e.stackTraceToString())
        return
    }

    if(HttpStatusCode.OK != response.status) {
        logger.error("An error occurred while accessing GitHub release endpoint. Code: ${response.status}. " +
                "Message: ${response.bodyAsText()}"
        )
        return
    }

    val responseParsed = try {
        json.decodeFromString<GitHubReleaseResponse>(response.bodyAsText())
    } catch (e: Exception) {
        logger.error("Error while parsing response body from GitHub.")
        logger.error(e.stackTraceToString())
        return
    }

    val latestVersion = responseParsed.name.substringAfter(textBeforeVersionNumber)

    if(BuildInfo.version != latestVersion) {
        logger.info("Found new Build version $latestVersion")
        BuildInfo.latestAvailableVersion = latestVersion
        BuildInfo.isNewVersionAvailable = true
    }

    var adjustedBodyText = responseParsed.body
        .split("\n").filter { !it.contains("**Full Changelog**") }

    if(adjustedBodyText.last().isBlank()) {
        adjustedBodyText = adjustedBodyText.toMutableList().dropLast(1)
    }

    BuildInfo.releaseBodyText = adjustedBodyText.joinToString("\n")
    BuildInfo.releaseAssets = responseParsed.assets
}


/**
 * Prepares and launches the automatic update process.
 *
 * Ensures required update files exist, downloads the update JAR if needed,
 * cleans up outdated versions, and starts the updater before terminating
 * the current application.
 *
 * @return false if preparation or execution fails
 */
fun prepareAndStartAutoUpdate(): Boolean {
    try {
        val updateJarNamePrefix = "Update_v"
        val updateJarName = "${updateJarNamePrefix}1.jar"
        val latestUpdateJarDownloadLink = "https://github.com/alexshadowolex/Spotify-Bot/releases/download/v2.0.5/$updateJarName"

        val binFolder = File("bin")
        val updateJar = File("${binFolder.path}\\$updateJarName")

        if(!binFolder.exists() || !binFolder.isDirectory) {
            binFolder.mkdir()
        }

        if(!updateJar.exists()) {
            updateJar.writeBytes(URL(latestUpdateJarDownloadLink).readBytes())

            binFolder.listFiles().filter {
                it.name.contains(updateJarNamePrefix) &&
                it.name != updateJarName &&
                it.extension == "jar"
            }.forEach {
                it.delete()
            }
        }

        val updateJarPath = updateJar.path
        val versionArg = BuildInfo.latestAvailableVersion
        val assetsArg = BuildInfo.releaseAssets.joinToString(";") { it.name + "," + it.browser_download_url }

        ProcessBuilder(
            "cmd", "/c",
            "start cmd /k \"java -jar $updateJarPath $versionArg $assetsArg && exit\""
        ).start()

        exitProcess(0)
    } catch (e: Exception) {
        logger.error("Error while starting auto-update: ", e)
    }

    return false
}