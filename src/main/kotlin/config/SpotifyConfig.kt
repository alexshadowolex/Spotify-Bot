package config
import CustomCommandPermissions
import displayEnumParsingErrorWindow
import getPropertyValue
import getSongIdFromSpotifyDirectLink
import logger
import java.io.File
import java.util.*
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

object SpotifyConfig {
    private val spotifyConfigFile = File("data\\properties\\spotifyConfig.properties")
    private val properties = Properties().apply {
        load(spotifyConfigFile.inputStream())
    }

    val spotifyClientSecret: String = File("data\\tokens\\spotifyClientSecret.txt").readText()
    val spotifyClientId: String = getPropertyValue(properties, "spotify_client_id", spotifyConfigFile.path)
    val playlistIdForAddSongCommand: String = getPropertyValue(
        properties, "playlist_id_for_add_song_command", spotifyConfigFile.path
    )
    var playlistNameForAddSongCommand = ""
    val addSongCommandSecurityLevelOnStartUp = try {
        CustomCommandPermissions.valueOf(
            getPropertyValue(properties, "add_song_command_security_level_on_start_up", spotifyConfigFile.path)
        )
    } catch (e: Exception) {
        displayEnumParsingErrorWindow(
            propertyName = "add_song_command_security_level_on_start_up",
            propertyFilePath = spotifyConfigFile.path,
            exception = e,
            enumClassValues = CustomCommandPermissions.values().map { it.toString() }
        )
        exitProcess(-1)
    }
    val maximumLengthMinutesSongRequest: Duration = try {
        getPropertyValue(properties, "maximum_length_minutes_song_request", spotifyConfigFile.path)
            .toDouble().minutes
    } catch (e: NumberFormatException) {
        logger.info(
            "Invalid number found while parsing property maximum_length_minutes_song_request, " +
            "setting to maximum length"
        )
        Double.MAX_VALUE.minutes
    }
    val blockedSongIds: List<String> = getPropertyValue(
        properties, "blocked_song_links", spotifyConfigFile.path
    ).split(",").map { getSongIdFromSpotifyDirectLink(it) ?: "" }
    val blockedSongArtists: List<String> = getPropertyValue(
        properties, "blocked_song_artists", spotifyConfigFile.path
    ).lowercase(Locale.getDefault()).split(",")
    val skipSongCommandSecurityLevelOnStartUp = try {
        CustomCommandPermissions.valueOf(
            getPropertyValue(properties, "skip_song_command_security_level_on_start_up", spotifyConfigFile.path)
        )

    } catch (e: Exception) {
        displayEnumParsingErrorWindow(
            propertyName = "skip_song_command_security_level_on_start_up",
            propertyFilePath = spotifyConfigFile.path,
            exception = e,
            enumClassValues = CustomCommandPermissions.values().map { it.toString() }
        )
        exitProcess(-1)
    }
    val customGroupUserNamesAddSongCommand: List<String> = getPropertyValue(
        properties, "custom_group_user_names_add_song_command", spotifyConfigFile.path
    ).lowercase(Locale.getDefault()).split(",")
    val customGroupUserNamesSkipSongCommand: List<String> = getPropertyValue(
        properties, "custom_group_user_names_skip_song_command", spotifyConfigFile.path
    ).lowercase(Locale.getDefault()).split(",")
    val removeSongFromQueueCommandSecurityLevelOnStartUp = try {
        CustomCommandPermissions.valueOf(
            getPropertyValue(
                properties,
                "remove_song_from_queue_command_security_level_on_start_up",
                spotifyConfigFile.path
            )
        )

    } catch (e: Exception) {
        displayEnumParsingErrorWindow(
            propertyName = "remove_song_from_queue_command_security_level_on_start_up",
            propertyFilePath = spotifyConfigFile.path,
            exception = e,
            enumClassValues = CustomCommandPermissions.values().map { it.toString() }
        )
        exitProcess(-1)
    }
    val customGroupUserNamesRemoveSongFromQueueCommand: List<String> = getPropertyValue(
        properties, "custom_group_user_names_remove_song_from_queue_command", spotifyConfigFile.path
    ).lowercase(Locale.getDefault()).split(",")
}