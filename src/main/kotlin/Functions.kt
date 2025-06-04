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
 * Sets up the connection to twitch
 * @param requestedByQueueHandler RequestedByQueueHandler-instance
 * @return the TwitchClient-object
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
 * Initiates the callback for the reward redeems
 * @param redeemEvent the redeem event
 * @param twitchClient the twitchClient
 * @param requestedByQueueHandler requestedByQueueHandler
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
 * Sets up the logging process with multiple output stream to both console and log file
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
 * Gets the value of the specified property out of the given properties-file. When an error occurred, the
 * function will display a descriptive error message windows and end the app.
 * @param properties already initialized properties-class
 * @param propertyName name of the property
 * @param propertiesFileRelativePath the relative path of the properties file
 * @return on success, the raw value of the property
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
 * Displays an error message window as JOptionPane.
 * @param message the message to display
 * @param title the title to display
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
 * Displays an error message window for invalid enum class values.
 * @param propertyName name of the property
 * @param propertyFilePath path to the property file
 * @param exception occurred exception while parsing the value
 * @param enumClassValues possible string-values of the enum property
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
 * Checks if the OS is windows and is in dark mode.
 * @return true, if OS is windows and in dark mode, else false
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
 * Checks if a user is the broadcaster, specified in TwitchBotConfig.properties channel property.
 * @param userName name of the user to check
 * @return true, if the user is the broadcaster, else false.
 */
fun isUserBroadcaster(userName: String): Boolean {
    return userName == TwitchBotConfig.channel
}


// Twitch Bot functions
/**
 * Checks if a user is blacklisted
 * @param userName user's Name
 * @param userId user's ID
 * @return true, if the user is blacklisted, else false
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
 * Helper function that sends a message to twitch chat and logs it
 * @param chat the twitch chat
 * @param message content of the message
 */
fun sendMessageToTwitchChatAndLogIt(chat: TwitchChat, message: String) {
    chat.sendMessage(TwitchBotConfig.channel, message)
    logger.info("Sent Twitch chat message: $message")
}


/**
 * Checks if a user is part of a custom group or the broadcaster.
 * @param userName user name
 * @param customGroup custom group to check
 * @return true, if the user is part of the custom group or the broadcaster, else false
 */
fun isUserPartOfCustomGroupOrBroadcaster(userName: String, customGroup: List<String>): Boolean {
    return userName == TwitchBotConfig.channel || customGroup.contains(userName.lowercase(Locale.getDefault()))
}


/**
 * Function that does all general sanity checks for commands without security level checks and handles the logging and
 * result communication via chat.
 * The following sanity checks are done:
 *      - Is the command enabled
 * @param commandName name of the command which is used for the logging
 * @param isCommandEnabledFlag the flag of the specific command
 * @param userName Name of the user to check
 * @param userID ID of the user to check
 * @param twitchClient TwitchClient-Object
 * @return true, if all sanity checks succeeded, else false
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
 * Function that does all general sanity checks for commands including security level checks and handles the logging and
 * result communication via chat.
 * Following sanity checks are done:
 *      - sanity checks of handleCommandSanityChecksWithoutSecurityLevel
 *      - security level checks
 * @param commandName name of the command which is used for the logging
 * @param isCommandEnabledFlag the flag of the specific command
 * @param messageEvent ChannelMessageEvent-Object
 * @param twitchClient TwitchClient-Object
 * @param securityCheckFunction security check function of the specific command
 * @param securityLevel current security setting of the command
 * @return true, if all sanity checks succeeded, else false
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
 * Checks if the given user ID is following the broadcaster.
 * @param userID ID of the user to check if they are following
 * @param twitchClient client to execute the request
 * @return null on error, true if the user following, else false
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
 * Helper function to get the follow-information of the given user for the broadcaster's channel
 * @param userID ID of the user to get the information from
 * @param twitchClient client to execute the request
 * @return List of InboundFollow-Object. It contents no or exactly one element. Null on error
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
 * Gets the duration a user is following since. If the user is not following, it will return the duration -1
 * @param userID ID of the user to get the following duration for
 * @param twitchClient client to execute the request
 * @return null on error, Duration -1 if the user is not following, else the
 * Duration the user is following since
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
 * Checks if a user is following long enough. The following Duration is compared to the property
 * TwitchBotConfig.minimumFollowingDurationMinutes
 * @param userID ID of the user to check if they are following long enough
 * @param twitchClient client to execute the request
 * @return null on error, true if the following duration is longer than the value in
 * TwitchBotConfig.minimumFollowingDurationMinutes, else false
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
 * This function gets called when the user-name of the creator "alexshadowolex", specified in
 * ALEX_TWITCH_USER_NAME, types the first message in chat and then praises him with a random message.
 * @param chat TwitchChat-object
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
 * Helper function to be called both by redeem and command.
 * It calls the updateQueue-function and issues a message
 * to twitch chat.
 * @param chat the twitch chat
 * @param query query or link
 * @return true on success, else false
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
 * Updates the spotify queue adding a song to it
 * @param query either a spotify link or a query that will be searched for
 * @return on success: track and explanation message, on failure: null and explanation message
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
 * Extracts the song ID from a spotify direct link.
 * @param directLink the spotify direct link as a String
 * @return the ID on success, null on failure
 */
fun getSongIdFromSpotifyDirectLink(directLink: String): String? {
    return Url(directLink).takeIf { isUrlSpotifyTrackDirectLink(it) }
        ?.encodedPath?.substringAfter("/track/")
}


/**
 * Gets the first (if there exist more than one included) blocked artist from the given spotify config property.
 * @param artists artist names
 * @return the name of the (first) blocked artist or an empty string, if no artist is blocked
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
 * Checks if a song is blocked.
 * @param songId ID of the song to check
 * @return true, if the song is blocked, else false
 */
private fun isSongBlocked(songId: String): Boolean {
    return SpotifyConfig.blockedSongLinks.map { getSongIdFromSpotifyDirectLink(it) ?: "" }.contains(songId)
}


/**
 * Checks if one or more artists are blocked. The function is not case-sensitive.
 * @param artists artist names
 * @return true, if at least one artist ist blocked, else false
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
 */
fun prepareAndStartAutoUpdate() {
    val updateJarNamePrefix = "Update_v"
    val updateJarName = "${updateJarNamePrefix}1.jar"
    val latestUpdateJarDownloadLink = "https://github.com/alexshadowolex/Spotify-Bot/releases/download/v2.0.5/$updateJarName"

    val binFolder = File("data\\bin")
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
}