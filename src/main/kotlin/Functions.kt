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
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent
import config.BuildInfo
import config.TwitchBotConfig
import handler.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup
import ui.addSongCommandSecurityLevel
import ui.isEmptySongDisplayFilesOnPauseEnabled
import ui.isSongRequestEnabledAsCommand
import ui.isSpotifySongNameGetterEnabled
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.time.format.DateTimeFormatterBuilder
import java.util.*
import javax.swing.JOptionPane
import kotlin.collections.set
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// Setup Twitch Bot
/**
 * Sets up the connection to twitch
 * @return {TwitchClient} the TwitchClient-class
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
        sendMessageToTwitchChatAndLogIt(this, "Bot running peepoArrive")
    }

    val nextAllowedCommandUsageInstantPerUser = mutableMapOf<Pair<Command, /* user: */ String>, Instant>()
    val nextAllowedCommandUsageInstantPerCommand = mutableMapOf<Command, Instant>()

    val channelId = try {
        twitchClient.helix.getUsers(
            TwitchBotConfig.chatAccountToken,
            null,
            listOf(TwitchBotConfig.channel)
        ).execute().users.first().id
    } catch (e: NoSuchElementException) {
        logger.error("An Error occurred with the channel name. Maybe the channel name is spelled wrong?")
        e.printStackTrace()
        throw ExceptionInInitializerError(
            "Error with channel name. Check the value of \"channel_name\" in the twitchBotConfig.properties-file!"
        )
    }

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

        val command = commands.find {
            parts.first().substringAfter(TwitchBotConfig.commandPrefix).lowercase() in it.names
        } ?: return@onEvent

        val userName = messageEvent.user.name
        val userId = messageEvent.user.id
        val chat = messageEvent.twitchChat

        logger.info(
            "User '$userName' tried using command '${command.names.first()}' with arguments: " +
            parts.drop(1).joinToString()
        )

        if(isUserBlacklisted(userName, userId)) {
            sendMessageToTwitchChatAndLogIt(
                chat,
                "Imagine not being a blacklisted user. Couldn't be you $userName ${TwitchBotConfig.blacklistEmote}"
            )
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
            userName != TwitchBotConfig.channel
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
            userName != TwitchBotConfig.channel
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
            chat = chat,
            messageEvent = messageEvent,
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
 * @param redeemEvent {RewardRedeemedEvent} the redeem event
 * @param twitchClient {TwitchClient} the twitchClient
 */
fun rewardRedeemEventHandler(redeemEvent: RewardRedeemedEvent, twitchClient: TwitchClient) {
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
 * Sets up the logging process with {MultiOutputStream} to both console and log file
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


// General functions
/**
 * Gets the value of the specified property out of the given properties-file. When an error occurres, the
 * function will display a descriptive error message windows and end the app.
 * @param properties {Properties} already initialized properties-class
 * @param propertyName {String} name of the property
 * @param propertiesFileRelativePath {String} the relative path of the properties file
 * @return {String} on success, the raw value of the property
 */
fun getPropertyValue(properties: Properties, propertyName: String, propertiesFileRelativePath: String): String {
    return try {
        properties.getProperty(propertyName)
    } catch (e: Exception) {
        logger.error("Exception occurred while reading property $propertyName in file $propertiesFileRelativePath: ", e)
        JOptionPane.showMessageDialog(
            null,
            "Error while reading value of property ${propertyName.addQuotationMarks()} " +
                    "in file $propertiesFileRelativePath.\n" +
                    "Check logs for more information",
            "Error while reading properties",
            JOptionPane.ERROR_MESSAGE
        )
        exitProcess(-1)
    }
}


/**
 * Checks if the OS is windows and is in dark mode.
 * @return {Boolean} true, if OS is windows and in dark mode, else false
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


// Twitch Bot functions
/**
 * Checks if a user is blacklisted
 * @param userName {String} user's Name
 * @param userId {String} user's ID
 * @return {Boolean} true, if the user is blacklisted, else false
 */
fun isUserBlacklisted(userName: String, userId: String): Boolean {
    if(userName in TwitchBotConfig.blacklistedUsers || userId in TwitchBotConfig.blacklistedUsers) {
        if(userId !in TwitchBotConfig.blacklistedUsers) {
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
 * @param chat {TitchChat} the twitch chat
 * @param message {String} content of the message
 */
fun sendMessageToTwitchChatAndLogIt(chat: TwitchChat, message: String) {
    chat.sendMessage(TwitchBotConfig.channel, message)
    logger.info("Sent Twitch chat message: $message")
}


// Spotify Functions
/**
 * Helper function to be called both by redeem and command. Calls the update queue and issues a message to twitch chat.
 * @param chat {TwitchChat} the twitch chat
 * @param query {String} query or link
 * @return {Boolean} true on success, else false
 */
suspend fun handleSongRequestQuery(chat: TwitchChat, query: String): Boolean {
    logger.info("called handleSongRequestQuery with query $query")
    var success = true
    try {
        val message = updateQueue(query).let { result ->
            val track = result.track
            if(track != null) {
                "Song ${track.name.addQuotationMarks()} by ${getArtistsString(track.artists)} has been added to the queue " +
                TwitchBotConfig.songRequestEmotes.random()
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
 * @param query {String} either a spotify link or a query that will be searched for
 * @return {SongRequestResult} {Track-Item?} and {String}, on success: track and explanation message,
 * on failure: null and explanation message
 */
private suspend fun updateQueue(query: String): SongRequestResult {
    logger.info("called updateQueue with query $query")
    val result = try {
        Url(query).takeIf { isQuerySpotifyTrackDirectLink(it) }?.let {
            val songId = it.encodedPath.substringAfter("/track/")
            getSpotifyTrackById(songId)
        } ?: run {
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

    if(result.length.milliseconds > SpotifyConfig.maximumLengthMinutesSongRequest) {
        logger.info("Song length ${result.length / 60000f} was longer than ${SpotifyConfig.maximumLengthMinutesSongRequest}")
        return SongRequestResult(
            track = null,
            songRequestResultExplanation = "The song was longer than ${SpotifyConfig.maximumLengthMinutesSongRequest}."
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
 * Checks if the given URL is a spotify direct link to a track.
 * @param query {Url} the given link
 * @return {Boolean} true, if it is a direct link to a spotify track, else false
 */
private fun isQuerySpotifyTrackDirectLink(query: Url): Boolean {
    return query.host == "open.spotify.com" && query.encodedPath.contains("/track/")
}


/**
 * Gets the track from the Spotify APIs track endpoint.
 * @param songId {String} the link's songId
 * @return {Track?} a track on success, null on error
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
 * Gets the track from the Spotify APIs search endpoint.
 * @param query {String} the search query
 * @return {Track?} a track on success, null on error
 */
private suspend fun getSpotifyTrackByQuery(query: String): Track? {
    logger.info("called getSpotifyTrackByQuery with query: $query")
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
 * @return {Track?} track, if successful, else null
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
 * @param name {String} name of the given song
 * @param artists {List<SimpleArtist>} artists of the given song
 * @return {String} song name and artists
 */
fun createSongString(name: String, artists: List<SimpleArtist>): String {
    return "${name.addQuotationMarks()} by ${getArtistsString(artists)}"
}


/**
 * Creates the concatenation of a list of artists with "," and the last 2 with "and"
 * @param artists {List<SimpleArtist>} artists
 * @return {String} concatenation of the artists
 */
fun getArtistsString(artists: List<SimpleArtist>): String {
    return artists.map { it.name }.let { artist ->
        listOf(
            artist.dropLast(1).joinToString(),
            artist.last()
        ).filter { it.isNotBlank() }.joinToString(" and ")
    }
}


private const val CURRENT_SONG_FILE_NAME = "currentSong.txt"
private const val CURRENT_SONG_NAME_FILE_NAME = "currentSongName.txt"
private const val CURRENT_SONG_ARTISTS_FILE_NAME = "currentSongArtists.txt"
private const val CURRENT_SONG_ALBUM_IMAGE_FILE_NAME = "currentAlbumImage.jpg"
private const val DISPLAY_FILES_DIRECTORY = "data\\displayFiles"
/**
 * Function that handles the coroutine to get the current spotify song.
 * On Start up it creates the dir and files, if needed.
 * If isSpotifySongNameGetterEnabled is true, it constantly does a GET-Request to get the currently playing
 * song name and writes it into a file
 * Delay for next pull is 2 seconds
 */
fun startSpotifySongGetter() {
    logger.info("called startSpotifySongGetter")
    backgroundCoroutineScope.launch {
        createSongDisplayFolderAndFiles()

        while(isActive) {
            if(!isSpotifySongNameGetterEnabled()) {
                emptyAllSongDisplayFiles()
                delay(0.5.seconds)
                continue
            }

            val isPlaying = if(isEmptySongDisplayFilesOnPauseEnabled.value) {
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

                downloadAndSaveAlbumImage(currentTrack)
                writeCurrentSongTextFiles(currentTrack)
            } else {
                emptyAllSongDisplayFiles()
            }

            delay(2.seconds)
        }
    }
}


/**
 * Helper function to outsource the try-catch block of accessing the variable isSpotifySongNameGetterEnabled
 * @return {Boolean} true, if the functionality is enabled. False, if not or an error occurred.
 */
private fun isSpotifySongNameGetterEnabled(): Boolean {
    return try {
        isSpotifySongNameGetterEnabled.value
    } catch (e: Exception) {
        false
    }
}


/**
 * Writes current song into the separate text files
 * @param currentTrack {Track} current Track
 */
private fun writeCurrentSongTextFiles(currentTrack: Track) {
    try {
        val currentSongString = createSongString(currentTrack.name, currentTrack.artists)
        File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_FILE_NAME")
            .writeText(currentSongString + " ".repeat(10))
        File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_NAME_FILE_NAME")
            .writeText(currentTrack.name)
        File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_ARTISTS_FILE_NAME")
            .writeText(getArtistsString(currentTrack.artists))
    } catch (e: Exception) {
        logger.error("Exception occurred while trying to save the song in files in writeCurrentSongTextFiles ", e)
    }
}


/**
 * Downloads the current song's album image
 * @param currentTrack {Track} current Track
 */
private fun downloadAndSaveAlbumImage(currentTrack: Track) {
    try {
        val images = currentTrack.album.images
        if (images.isNotEmpty()) {
            val imageUrl = images.first().url
            val imageData = URL(imageUrl).readBytes()
            File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_ALBUM_IMAGE_FILE_NAME").writeBytes(imageData)
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
            File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_ALBUM_IMAGE_FILE_NAME")
        ).forEach { currentFile ->
            if (!currentFile.exists()) {
                withContext(Dispatchers.IO) {
                    currentFile.createNewFile()
                    logger.info("Created current song display file ${currentFile.name}")
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
        File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_ARTISTS_FILE_NAME")
    ).forEach{ currentFile ->
        currentFile.writeText("")
    }

    File("$DISPLAY_FILES_DIRECTORY\\$CURRENT_SONG_ALBUM_IMAGE_FILE_NAME").writeBytes(
        object {}.javaClass.getResourceAsStream("Blank.jpg")!!.readAllBytes()
    )
}


/**
 * Checks if song request redeem is enabled. This is the case when song request command is not enabled.
 * @return {Boolean} true, if song request redeem is enabled, else false
 */
fun isSongRequestEnabledAsRedeem(): Boolean {
    return !isSongRequestEnabledAsCommand.value
}


/**
 * Checks spotify api if the player is playing.
 * For reference of what the return codes mean, check here:
 * https://developer.spotify.com/documentation/web-api/reference/get-information-about-the-users-current-playback
 * @return {Boolean?} true, if the player is playing. False, if the player is not playing or not active. Null on error.
 */
suspend fun isSpotifyPlaying(): Boolean? {
    val playbackEndpoint = "https://api.spotify.com/v1/me/player"
    val json = Json { ignoreUnknownKeys = true }

    val response = httpClient.get(playbackEndpoint) {
        header("Authorization", "Bearer ${spotifyClient.token.accessToken}")
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
 * @param song {Track} the song to add
 * @param playlistId {String} playlist's ID
 * @return {Boolean} true on success, else false
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
 * Checks if the user's permissions are eligible for using the add song command.
 * @param permissions {Set<CommandPermission>} permissions of current user
 * @return {Boolean} true, if the user is eligible, else false
 */
fun areUsersPermissionsEligibleForAddSongCommand(permissions: Set<CommandPermission>): Boolean {
    logger.info("called areUsersPermissionsEligibleForAddSongCommand")
    return permissions.contains(addSongCommandSecurityLevel.value)
}


/**
 * Issues a GET-Request to spotify API to get the playlist's name of the given playlist ID
 * @param playlistId {String} playlist's ID to get the name of
 * @return {String} the name on success, empty String on failure
 */
suspend fun getPlaylistName(playlistId: String): String {
    return spotifyClient.playlists.getPlaylist(playlistId)?.name ?: ""
}


/**
 * Checks if a song is in a given playlist by ID
 * @param songId {String} the song's ID to check for
 * @param playlistId {String} the playlist's ID
 * @return {Boolean} true, if the playlist contains the song, else false
 */
suspend fun isSongInPlaylist(songId: String, playlistId: String): Boolean {
    val playlistSongIds = getPlaylistSongIds(playlistId)
    return playlistSongIds.contains(songId)
}


/**
 * Gets all the song IDs of the specified playlist's songs. It issues a GET-Request to spotify api for every 100
 * songs contained in that playlist.
 * @param playlistId {String} ID of the playlist to get the song IDs of
 * @return {List<String?>} IDs of the songs in that playlist, empty strings if issues occurred
 */
suspend fun getPlaylistSongIds(playlistId: String): List<String?> {
    val playlistSongIds = mutableListOf<String?>()
    val limit = 100
    var currentOffset = 0
    var nextLink: String? = ""

    while(nextLink != null) {
        val result = spotifyClient.playlists.getPlaylistTracks(
            playlist = playlistId,
            offset = currentOffset,
            limit = limit
        )

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
 * @param song {Track} song to add
 * @return {String} message to display in twitch chat afterward
 */
suspend fun handleAddSongCommandFunctionality(song: Track): String {
    return if(isSongInPlaylist(song.id, SpotifyConfig.playlistIdForAddSongCommand)) {
        "Song ${song.name.addQuotationMarks()} is already in playlist "
    } else {
        addSongToPlaylist(song, SpotifyConfig.playlistIdForAddSongCommand)
        "Successfully added song ${song.name.addQuotationMarks()} to the playlist "
    } + getAddSongPlaylistNameString()
}


/**
 * Gets the playlist's name from the add Song Command. Since the property might still be empty, this needs to get
 * checked and if so, try and fill the property with the correct name.
 * @return {String} the playlist's name. If the property is not empty, it will be surrounded by quotation marks
 */
suspend fun getAddSongPlaylistNameString(): String {
    if(SpotifyConfig.playlistNameForAddSongCommand.isEmpty()) {
        SpotifyConfig.playlistNameForAddSongCommand = getPlaylistName(SpotifyConfig.playlistIdForAddSongCommand)
    }

    return SpotifyConfig.playlistNameForAddSongCommand.addQuotationMarks()
}


// Github
const val GITHUB_LATEST_VERSION_LINK = "https://github.com/alexshadowolex/Spotify-Bot/releases/latest"
/**
 * Checks GitHub to see if a new version of this app is available
 * @return {Boolean} true, if there is a new version, else false
 */
fun isNewAppReleaseAvailable(): Boolean {
    logger.info("called isNewAppReleaseAvailable")
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
        logger.info("Found new Build version $latestVersion")
        BuildInfo.latestAvailableVersion = latestVersion
        return true
    }

    return false
}