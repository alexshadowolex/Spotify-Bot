package config
import getPropertyValue
import joinToPropertiesString
import logger
import showErrorMessageWindow
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

object SpotifyConfig {
    private val spotifyConfigFile = File("data\\properties\\spotifyConfig.properties")
    private val properties = Properties().apply {
        if(!spotifyConfigFile.exists()) {
            logger.error(
                "Error while reading property file ${spotifyConfigFile.path} in SpotifyConfig init: " +
                "File does not exist!"
            )
            showErrorMessageWindow(
                title = "Error while reading properties file",
                message = "Property file \"${spotifyConfigFile.path}\" does not exist!"
            )

            exitProcess(-1)
        }
        load(spotifyConfigFile.inputStream())
    }

    val spotifyClientSecret: String = File("data\\tokens\\spotifyClientSecret.txt").readText()

    val spotifyClientId: String = getPropertyValue(properties, "spotify_client_id", spotifyConfigFile.path)

    var playlistIdForAddSongCommand: String = getPropertyValue(
        properties, "playlist_id_for_add_song_command", spotifyConfigFile.path
    )
        set(value) {
            field = value
            properties.setProperty("playlist_id_for_add_song_command", value)
            savePropertiesToFile()
        }

    var playlistNameForAddSongCommand = ""

    var maximumLengthMinutesSongRequest: Duration = try {
        getPropertyValue(properties, "maximum_length_minutes_song_request", spotifyConfigFile.path)
            .toDouble().minutes
    } catch (e: NumberFormatException) {
        logger.info(
            "Invalid number found while parsing property maximum_length_minutes_song_request, " +
            "setting to maximum length"
        )
        Double.MAX_VALUE.minutes
    }
        set(value) {
            field = value
            properties.setProperty(
                "maximum_length_minutes_song_request", value.toDouble(DurationUnit.MINUTES).toString()
            )
            savePropertiesToFile()
        }

    // TODO add configListSeparator="," to BotConfig.properties
    var blockedSongLinks: List<String> = getPropertyValue(
        properties, "blocked_song_links", spotifyConfigFile.path
    ).split(",")
        set(value) {
            field = value
            properties.setProperty("blocked_song_links", value.joinToPropertiesString(","))
            savePropertiesToFile()
        }

    var blockedSongArtists: List<String> = getPropertyValue(
        properties, "blocked_song_artists", spotifyConfigFile.path
    ).lowercase(Locale.getDefault()).split(",")
        set(value) {
            field = value
            properties.setProperty(
                "blocked_song_artists", value.joinToPropertiesString(",").lowercase(Locale.getDefault())
            )
            savePropertiesToFile()
        }

    private fun savePropertiesToFile() {
        properties.store(FileOutputStream(spotifyConfigFile.path), null)
    }
}