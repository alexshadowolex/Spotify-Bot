
import com.github.twitch4j.common.enums.CommandPermission
import java.io.File
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

object SpotifyConfig {
    private val properties = Properties().apply {
        load(File("data\\properties\\spotifyConfig.properties").inputStream())
    }

    val spotifyClientSecret: String = File("data\\tokens\\spotifyClientSecret.txt").readText()
    val spotifyClientId: String = properties.getProperty("spotify_client_id")
    val playlistIdForAddSongCommand: String = properties.getProperty("playlist_id_for_add_song_command")
    val addSongCommandSecurityLevelOnStartUp = CommandPermission.valueOf(properties.getProperty("add_song_command_security_level_on_start_up"))
    val maximumLengthMinutesSongRequest: Duration = try {
        properties.getProperty("maximum_length_minutes_song_request").toDouble().minutes
    } catch (e: NumberFormatException) {
        logger.info("Invalid number found while parsing property maximum_length_minutes_song_request, setting to maximum length")
        Double.MAX_VALUE.minutes
    }
}