import com.adamratzman.spotify.endpoints.pub.SearchApi
import com.adamratzman.spotify.models.Track
import com.adamratzman.spotify.utils.Market
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.TwitchClient
import com.github.twitch4j.TwitchClientBuilder
import com.github.twitch4j.chat.TwitchChat
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent
import config.BuildInfo
import config.TwitchBotConfig
import handler.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import org.jsoup.Jsoup
import ui.isSpotifySongNameGetterEnabled
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths
import java.time.format.DateTimeFormatterBuilder
import kotlin.collections.drop
import kotlin.collections.dropLast
import kotlin.collections.filter
import kotlin.collections.find
import kotlin.collections.first
import kotlin.collections.firstOrNull
import kotlin.collections.getOrPut
import kotlin.collections.joinToString
import kotlin.collections.last
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.random
import kotlin.collections.set
import kotlin.time.Duration.Companion.seconds

// Setup Twitch Bot
/**
 * Sets up the connection to twitch
 * @return TwitchClient the TwitchClient-class
 */
suspend fun setupTwitchBot(): TwitchClient {
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
        sendMessage(TwitchBotConfig.channel, "Bot running peepoArrive")
    }

    val nextAllowedCommandUsageInstantPerUser = mutableMapOf<Pair<Command, /* user: */ String>, Instant>()
    val nextAllowedCommandUsageInstantPerCommand = mutableMapOf<Command, Instant>()

    val channelId = twitchClient.helix.getUsers(
        TwitchBotConfig.chatAccountToken,
        null,
        listOf(TwitchBotConfig.channel)
    ).execute().users.first().id
    twitchClient.pubSub.listenForChannelPointsRedemptionEvents(
        oAuth2Credential,
        channelId
    )

    twitchClient.pubSub.listenForChannelPointsRedemptionEvents(oAuth2Credential, channelId)

    twitchClient.eventManager.onEvent(RewardRedeemedEvent::class.java) { rewardRedeemEvent ->
        rewardRedeemEventHandler(rewardRedeemEvent, twitchClient)
    }

    twitchClient.eventManager.onEvent(ChannelMessageEvent::class.java) { messageEvent ->
        val message = messageEvent.message
        if (!message.startsWith(TwitchBotConfig.commandPrefix)) {
            return@onEvent
        }

        val parts = message.substringAfter(TwitchBotConfig.commandPrefix).split(" ")

        val command = commands.find { parts.first().substringAfter(TwitchBotConfig.commandPrefix).lowercase() in it.names } ?: return@onEvent

        logger.info("User '${messageEvent.user.name}' tried using command '${command.names.first()}' with arguments: ${parts.drop(1).joinToString()}")

        if(isUserBlacklisted(messageEvent.user.name, messageEvent.user.id, twitchClient.chat)) {
            return@onEvent
        }

        val nextAllowedCommandUsageInstant = nextAllowedCommandUsageInstantPerCommand.getOrPut(command) {
            Clock.System.now()
        }

        val nextAllowedCommandUsageInstantForUser = nextAllowedCommandUsageInstantPerUser.getOrPut(command to messageEvent.user.name) {
            Clock.System.now()
        }
        if((Clock.System.now() - nextAllowedCommandUsageInstant).isNegative() && messageEvent.user.name != TwitchBotConfig.channel) {
            val secondsUntilTimeoutOver = (nextAllowedCommandUsageInstant - Clock.System.now()).inWholeSeconds.seconds

            twitchClient.chat.sendMessage(TwitchBotConfig.channel, "Command is still on cooldown. Please try again in $secondsUntilTimeoutOver")

            return@onEvent
        }

        if ((Clock.System.now() - nextAllowedCommandUsageInstantForUser).isNegative() && messageEvent.user.name != TwitchBotConfig.channel) {
            val secondsUntilTimeoutOver = (nextAllowedCommandUsageInstantForUser - Clock.System.now()).inWholeSeconds.seconds

            twitchClient.chat.sendMessage(TwitchBotConfig.channel, "You are still on cooldown. Please try again in $secondsUntilTimeoutOver")

            return@onEvent
        }

        val commandHandlerScope = CommandHandlerScope(
            chat = twitchClient.chat,
            messageEvent = messageEvent,
        )

        backgroundCoroutineScope.launch {
            command.handler(commandHandlerScope, parts.drop(1))

            val key = command to messageEvent.user.name
            nextAllowedCommandUsageInstantPerUser[key] = Clock.System.now() + commandHandlerScope.addedUserCoolDown

            nextAllowedCommandUsageInstantPerCommand[command] = Clock.System.now() + commandHandlerScope.addedCommandCoolDown
        }
    }

    logger.info("Twitch client started.")
    return twitchClient
}


/**
 * Initiates the callback for the reward redeems
 * @param redeemEvent The event-class
 * @param twitchClient The TwitchClient-class
 */
fun rewardRedeemEventHandler(redeemEvent: RewardRedeemedEvent, twitchClient: TwitchClient) {
    val redeem = redeems.find { redeemEvent.redemption.reward.id in it.id || redeemEvent.redemption.reward.title in it.id }.also {
        if (it != null) {
            if(redeemEvent.redemption.reward.title in it.id) {
                logger.warn("Redeem ${redeemEvent.redemption.reward.title}. Please use following ID in the properties file instead of the name: ${redeemEvent.redemption.reward.id}")
            }
        }
    } ?: return

    if(isUserBlacklisted(redeemEvent.redemption.user.displayName, redeemEvent.redemption.user.id, twitchClient.chat)) {
        return
    }

    val redeemHandlerScope = RedeemHandlerScope(
        chat = twitchClient.chat,
        redeemEvent = redeemEvent
    )

    backgroundCoroutineScope.launch {
        redeem.handler(redeemHandlerScope, redeemEvent.redemption.userInput)
    }
}

// Logging
private const val LOG_DIRECTORY = "logs"
/**
 * Sets up the logging process with MultiOutputStream to both console and log file
 */
fun setupLogging() {
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

    logger.info("Log file '${logFile.name}' has been created.")
}


// Twitch Bot functions
/**
 * Checks if a user is blacklisted
 * @param userName User's Name
 * @param userId User's ID
 * @param chat Chat-Class
 * @return Boolean true, if the user is blacklisted, else false
 */
fun isUserBlacklisted(userName: String, userId: String, chat: TwitchChat): Boolean {

    if(userName in TwitchBotConfig.blacklistedUsers || userId in TwitchBotConfig.blacklistedUsers){

        chat.sendMessage(TwitchBotConfig.channel, "Imagine not being a blacklisted user. Couldn't be you $userName ${TwitchBotConfig.blacklistEmote}")
        if(userId !in TwitchBotConfig.blacklistedUsers) {
            logger.warn("Blacklisted user $userName tried using a command. Please use following ID in the properties file instead of the name: $userId")
        }
        return true
    }
    return false
}


// Spotify Functions
/**
 * Helper function to be called both by redeem and command. Calls the update queue and issues a message to twitch chat.
 * @param chat Twitch chat class
 * @param query given String-query, either link or pure string
 * @return Boolean true on success, else false
 */
suspend fun handleSongRequestQuery(chat: TwitchChat, query: String): Boolean {
    var success = true
    try {
        chat.sendMessage(
            TwitchBotConfig.channel,
            updateQueue(query).let { response ->
                val track = response.track
                if(track != null) {
                    "Song '${track.name}' by ${
                        track.artists.map { "'${it.name}'" }.let { artists ->
                            listOf(
                                artists.dropLast(1).joinToString(),
                                artists.last()
                            ).filter { it.isNotBlank() }.joinToString(" and ")
                        }
                    } has been added to the playlist ${TwitchBotConfig.songRequestEmotes.random()}"
                } else {
                    success = false
                    "Couldn't add song to the queue. ${response.songRequestResultExplanation}"
                }
            }
        )
    } catch (e: Exception) {
        logger.error("Something went wrong with songrequests", e)
        success = false
    }

    return success
}


/**
 * Updates the spotify queue adding a song to it
 * @param query Either a spotify link or a string that will be searched for
 * @return SongRequestResult A Track-Item? and a String, on success: Track item and message,
 * on failure: null and explanation
 */
private suspend fun updateQueue(query: String): SongRequestResult {
    val result = try {
        Url(query).takeIf { it.host == "open.spotify.com" && it.encodedPath.contains("/track/") }?.let {
            val songId = it.encodedPath.substringAfter("/track/")
            logger.info("Song ID from link: $songId")
            spotifyClient.tracks.getTrack(
                track = songId,
                market = Market.DE
            )
        } ?: run {
            spotifyClient.search.search(
                query = query,
                searchTypes = arrayOf(
                    SearchApi.SearchType.Artist,
                    SearchApi.SearchType.Album,
                    SearchApi.SearchType.Track
                ),
                market = Market.DE
            ).tracks?.firstOrNull()
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

    logger.info("Result after search: $result")

    try {
        spotifyClient.player.addItemToEndOfQueue(result.uri)
        logger.info("Result URI: ${result.uri.uri}")
    } catch (e: Exception) {
        logger.error("Spotify is probably not set up.", e)
        return SongRequestResult(
            track = null,
            songRequestResultExplanation = "Adding the song to the playlist failed."
        )
    }

    return SongRequestResult(
        track = result,
        songRequestResultExplanation = "Successfully added the song to the playlist."
    )
}

private data class SongRequestResult(
    val track: Track?,
    val songRequestResultExplanation: String
)


/**
 * Issues a GET-Request to get the currently playing spotify song
 * @return Track? The track-item, if successful, else null
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
 * @param song Track-Class of the given song
 * @return String song name and artists as a string
 */
fun createSongString(song: Track): String {
    return "\"${song.name}\"" +
            " by " +
            song.artists.map { it.name }.let { artists ->
                listOf(
                    artists.dropLast(1).joinToString(),
                    artists.last()
                ).filter { it.isNotBlank() }.joinToString(" and ")
            }
}


private const val CURRENT_SONG_FILE_NAME = "currentSong.txt"
private const val DISPLAY_FILES_DIRECTORY = "data\\displayFiles"
/**
 * Function that handles the coroutine to get the current spotify song name.
 * On Start up it creates the dir and files, if needed.
 * If isSpotifySongNameGetterEnabled is true, it constantly does a GET-Request to get the currently playing
 * song name and writes it into a file
 * Delay for next pull is 2 seconds
 */
fun startSpotifySongNameGetter() {
    backgroundCoroutineScope.launch {
        val displayFilesDirectory = File(DISPLAY_FILES_DIRECTORY)
        if(!displayFilesDirectory.exists() || !displayFilesDirectory.isDirectory) {
            displayFilesDirectory.mkdirs()
            logger.info("Created display file folder $DISPLAY_FILES_DIRECTORY")
        }
        val currentSongFile = File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_FILE_NAME")
        var currentFileContent: String
        if(!currentSongFile.exists()) {
            withContext(Dispatchers.IO) {
                currentSongFile.createNewFile()
                logger.info("Created current song display file $CURRENT_SONG_FILE_NAME")
            }
        }

        while(isActive) {
            if(isSpotifySongNameGetterEnabled) {
                val currentTrack = getCurrentSpotifySong()
                if (currentTrack == null) {
                    delay(1.seconds)
                    continue
                }

                val currentSongString = createSongString(currentTrack)

                currentFileContent = currentSongString
                currentSongFile.writeText(currentFileContent + " ".repeat(10))
                delay(2.seconds)
            } else {
                delay(0.5.seconds)
            }
        }
    }
}


// Github
const val GITHUB_LATEST_VERSION_LINK = "https://github.com/alexshadowolex/Spotify-Bot/releases/latest"
/**
 * Checks GitHub to see if a new version of this app is available
 * @return Boolean true, if there is a new version, else false
 */
fun isNewAppReleaseAvailable(): Boolean {
    // response = httpClient.get("https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest'") {}
    // TODO: at some point use the api of github with get request, as soon as we find out how to use it without auth
    val titleTagName = "title"
    val textBeforeVersionNumber = "Release v"
    val delimiterAfterVersionNumber = " "

    val latestVersion = Jsoup.connect(GITHUB_LATEST_VERSION_LINK).get()
        .select(titleTagName).first()?.text()
        ?.substringAfter(textBeforeVersionNumber)
        ?.substringBefore(delimiterAfterVersionNumber) ?: BuildInfo.version

    if(BuildInfo.version != latestVersion) {
        BuildInfo.latestAvailableVersion = latestVersion
        return true
    }

    return false
}