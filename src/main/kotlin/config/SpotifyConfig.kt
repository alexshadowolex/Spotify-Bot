
import com.github.twitch4j.common.enums.CommandPermission
import java.io.File
import java.util.*
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
    val addSongCommandSecurityLevelOnStartUp = CommandPermission.valueOf(
        getPropertyValue(properties, "add_song_command_security_level_on_start_up", spotifyConfigFile.path)
    )
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
}