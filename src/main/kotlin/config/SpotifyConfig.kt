
import com.github.twitch4j.common.enums.CommandPermission
import java.io.File
import java.util.*

object SpotifyConfig {
    private val properties = Properties().apply {
        load(File("data\\properties\\spotifyConfig.properties").inputStream())
    }

    val spotifyClientSecret: String = File("data\\tokens\\spotifyClientSecret.txt").readText()
    val spotifyClientId: String = properties.getProperty("spotify_client_id")
    val playlistIdForAddSongCommand: String = properties.getProperty("playlist_id_for_add_song_command")
    val addSongCommandSecurityLevelOnStartUp = CommandPermission.valueOf(properties.getProperty("add_song_command_security_level_on_start_up"))
}