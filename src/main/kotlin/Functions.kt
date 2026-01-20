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
 * If the property cannot be read, displays an error dialog and terminates the application.
 *
 * @param properties the [Properties] instance to read from
 * @param propertyName the key of the property
 * @param propertiesFileRelativePath the relative path of the properties file (used in error messages)
 * @return the raw value of the property as a [String]
 */
fun getPropertyValue(properties: Properties, propertyName: String, propertiesFileRelativePath: String): String {
    return try {
        properties.getProperty(propertyName)
    } catch (e: Exception) {
        logger.error("Exception occurred while reading property $propertyName in file $propertiesFileRelativePath: ", e)
        showErrorMessageWindow(
            message =   "Error while reading value of property ${propertyName.addQuotationMarks()} " +
                        "in file $propertiesFileRelativePath.",
            title = "Error while reading properties"
        )
        exitProcess(-1)
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

// TODO HERE
/**
 * Checks if the given URL is a spotify direct link to a track.
 * @param url the given URL
 * @return true, if it is a spotify direct link to a track, else false
 */
private fun isUrlSpotifyTrackDirectLink(url: Url): Boolean {
    return isUrlSpotifyDirectLink(url) && url.encodedPath.contains("/track/")
}


/**
 * Checks if the given URL is a spotify direct link to anything.
 * @param url the given URL
 * @return true, if it is a spotify direct link to anything, else false
 */
private fun isUrlSpotifyDirectLink(url: Url): Boolean {
    return url.host == "open.spotify.com"
}


/**
 * Gets the track from the Spotify APIs track endpoint.
 * @param songId the link's songId
 * @return a track on success, null on error
 */
private suspend fun getSpotifyTrackById(songId: String): Track? {
    logger.info("called getSpotifyTrackById with ID: $songId")
    return try {
        spotifyClient.tracks.getTrack(
            track = songId,
            market = Market.DE
        )
    } catch (e: Exception) {
        logger.error("Exception while accessing tracks endpoint of spotify: ", e)
        null
    }
}


/**
 * Gets the track from the Spotify APIs search endpoint. If the query is a spotify direct link to something
 * but not to a track, it will not search for a track to reduce random results.
 * @param query the search query
 * @return a track on success, null on error
 */
private suspend fun getSpotifyTrackByQuery(query: String): Track? {
    logger.info("called getSpotifyTrackByQuery with query: $query")
    if(isUrlSpotifyDirectLink(Url(query))) {
        logger.info("Query is a spotify direct link to something but not to a track. Aborting the search")
        return null
    }

    return try {
        spotifyClient.search.search(
            query = query,
            searchTypes = arrayOf(
                SearchApi.SearchType.Artist,
                SearchApi.SearchType.Album,
                SearchApi.SearchType.Track
            ),
            market = Market.DE
        ).tracks?.firstOrNull()
    } catch (e: Exception) {
        logger.error("Exception while accessing search endpoint of spotify: ", e)
        null
    }
}


/**
 * Issues a GET-Request to get the currently playing spotify song. If the player is not active,
 * the request will run until a TimeoutException occurred.
 * @return track, if successful, else null
 */
suspend fun getCurrentSpotifySong(): Track? {
    return try {
        spotifyClient.player.getCurrentlyPlaying()?.item as Track
    } catch (_: Exception) {
        null
    }
}


/**
 * Creates a string from the given song with Title and Artists
 * @param name name of the given song
 * @param artists artists of the given song
 * @return song name and artists
 */
fun createSongString(name: String, artists: List<SimpleArtist>): String {
    return "${name.addQuotationMarks()} by ${getArtistsString(artists)}"
}


/**
 * Creates the concatenation of a list of artists with "," and the last 2 with "and"
 * @param artists artists
 * @return concatenation of the artists
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
 * Function that handles the coroutine to get the current spotify song.
 * On Start up it creates the dir and files, if needed.
 * If isSpotifySongNameGetterEnabled is true, it constantly does a GET-Request to get the currently playing
 * song name and writes it into a file.
 * The delay for the next pull is 2 seconds.
 * @param requestedByQueueHandler RequestedByQueueHandler-instance
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
 * Helper function to outsource the try-catch block of accessing the variable isSpotifySongNameGetterEnabled
 * @return true, if the functionality is enabled. False, if not or an error occurred.
 */
fun isSpotifySongNameGetterEnabled(): Boolean {
    return try {
        BotConfig.isSpotifySongNameGetterEnabled
    } catch (_: Exception) {
        false
    }
}


/**
 * Writes current song into the separate text files. If the song is requested by a user, which is indicated
 * the variable currentRequestedByUsername not being null, an extra file will be filled with the content
 * "requested by <username>"
 * @param currentTrack current Track
 * @param currentRequestedByUsername the current requested by username
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
 * Downloads the current song's album image. If the image is not the default size 640x640 pixels, it gets scaled
 * to be the default size.
 * @param currentTrack current Track
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
 * Creates the song display folder and the files
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
 * Empties all song display files. Text files get the value of the string "", picture file gets
 * the value of a 1x1 white picture.
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
 * Checks if song request redeem is enabled. This is the case when song request command is not enabled.
 * @return true, if song request redeem is enabled, else false
 */
fun isSongRequestEnabledAsRedeem(): Boolean {
    return !BotConfig.isSongRequestCommandEnabled
}


/**
 * Helper function to check if the song request command is active. This is the case when both song requests
 * generally are enabled and the song requests are used as commands.
 * @return true, if both flags are true, else false
 */
fun isSongRequestCommandActive(): Boolean {
    return BotConfig.isSongRequestCommandEnabled && BotConfig.isSongRequestEnabled
}


/**
 * Helper function to check if the song request redeem is active. This is the case when both song requests
 * generally are enabled and the song requests are used as redeems.
 * @return true, if both flags are true, else false
 */
fun isSongRequestRedeemActive(): Boolean {
    return isSongRequestEnabledAsRedeem() && BotConfig.isSongRequestEnabled
}


/**
 * Checks spotify api if the player is playing.
 * For reference to what the return codes mean, check here:
 * https://developer.spotify.com/documentation/web-api/reference/get-information-about-the-users-current-playback
 * @return true, if the player is playing. False, if the player is not playing or not active. Null on error.
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
 * Adds a song to the playlist with the given playlist ID
 * @param song the song to add
 * @param playlistId playlist's ID
 * @return true on success, else false
 */
suspend fun addSongToPlaylist(song: Track, playlistId: String): Boolean {
    logger.info("called addSongToPlaylist")
    var success = true
    try {
        spotifyClient.playlists.addPlayableToClientPlaylist(
            playlistId,
            song.uri
        )
    } catch (e: SpotifyException.BadRequestException) {
        logger.error("Something went wrong when adding song to the playlist in addSongToPlaylist: ", e)
        success = false
    }

    return success
}


/**
 * Checks if the user is eligible for using the add song command. The eligibility is set
 * in the parameter addSongCommandSecurityLevel
 * @param permissions permissions of current user
 * @param userName username of the user
 * @return true, if the user is eligible, else false
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
 * Issues a GET-Request to spotify API to get the playlist's name of the given playlist ID
 * @param playlistId playlist's ID to get the name of
 * @return the name on success, empty String on failure
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
 * Checks if the given playlist ID is valid.
 * @param playlistId playlist ID to check for
 * @return true if the ID is valid, else and on error false
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
 * Checks if a song is in a given playlist by ID
 * @param songId the song's ID to check for
 * @param playlistId the playlist's ID
 * @return true, if the playlist contains the song, else false, null on error
 */
suspend fun isSongInPlaylist(songId: String, playlistId: String): Boolean? {
    val playlistSongIds = getPlaylistSongIds(playlistId) ?: return null
    return playlistSongIds.contains(songId)
}


/**
 * Gets all the song IDs of the specified playlist's songs. It issues a GET-Request to spotify api for every 100
 * songs contained in that playlist.
 * @param playlistId ID of the playlist to get the song IDs of
 * @return IDs of the songs in that playlist, empty strings if issues occurred, null if issues
 * with the playlist in general occurred
 */
suspend fun getPlaylistSongIds(playlistId: String): List<String?>? {
    val playlistSongIds = mutableListOf<String?>()
    val limit = 100
    var currentOffset = 0
    var nextLink: String? = ""

    while(nextLink != null) {
        val result = try {
            spotifyClient.playlists.getPlaylistTracks(
                playlist = playlistId,
                offset = currentOffset,
                limit = limit
            )
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
 * Handles the functionality of the add song command after all sanity checks succeeded.
 * Checks if the song is already in the give playlist. If so, it will not be added. If not, it adds the song
 * to the playlist specified per ID in SpotifyConfig.playlistIdForAddSongCommand.
 * @param song song to add
 * @return message to display in twitch chat afterward
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
 * Gets the playlist's name from the add Song Command. Since the property might still be empty, this needs to get
 * checked and if so, try and fill the property with the correct name.
 * @return the playlist's name. If the property is not empty, it will be surrounded by quotation marks
 */
suspend fun getAddSongPlaylistNameString(): String {
    if(SpotifyConfig.playlistNameForAddSongCommand.isEmpty()) {
        SpotifyConfig.playlistNameForAddSongCommand = getPlaylistName(SpotifyConfig.playlistIdForAddSongCommand)
    }

    return SpotifyConfig.playlistNameForAddSongCommand.addQuotationMarks()
}


/**
 * Checks if the user is eligible for using the skip song command. The eligibility is set
 * in the parameter skipSongCommandSecurityLevel
 * @param permissions permissions of current user
 * @param userName username of the user
 * @return true, if the user is eligible, else false
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
 * Checks if the user is eligible for using the remove song from queue command. The eligibility is set
 * in the parameter removeSongFromQueueCommandSecurityLevel
 * @param permissions permissions of current user
 * @param userName username of the user
 * @return true, if the user is eligible, else false
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
 * Checks if the user is eligible for using the block song command. The eligibility is set
 * in the parameter blockSongCommandSecurityLevel
 * @param permissions permissions of current user
 * @param userName username of the user
 * @return true, if the user is eligible, else false
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
 * Helper function for the isUserEligibleForXYZCommand functions. Checks if the user is eligible with
 * the command's specific security level. If the value CUSTOM is selected, it checks whether the user
 * is the broadcaster or part of the custom group. If the other two values are selected, it checks
 * the user's permissions instead.
 * @param permissions permissions of current user
 * @param userName username of the user
 * @param commandSecurityLevel variable that holds the command's current security level
 * @param customGroup list of the command's custom usernames
 * @return true, if the user is eligible, else false
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


// Github
const val GITHUB_LATEST_VERSION_LINK = "https://github.com/alexshadowolex/Spotify-Bot/releases/latest"


/**
 * Gets and saves the latest release information from GitHub. The information is saved in BuildInfo.
 * Information that will be saved is:
 *  - latest available version
 *  - release body text
 *  - release assets
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
 * This functions prepares and starts the auto update-script. If the bin-folder is not existing, it will be created.
 * If the specified version of the Update-Jar is not existing, it will be downloaded and older versions of it will
 * be deleted.
 * In the end, the update-script will be executed and the Spotify-Bot will be closed.
 * @return false, if the preparation or start of the auto update was not successful
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