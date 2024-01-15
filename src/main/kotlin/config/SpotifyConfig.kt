
import java.io.File
import java.util.*

object SpotifyConfig {
    private val properties = Properties().apply {
        load(File("data\\properties\\spotifyConfig.properties").inputStream())
    }

    val spotifyClientSecret: String = File("data\\tokens\\spotifyClientSecret.txt").readText()
    val spotifyClientId: String = properties.getProperty("spotify_client_id")
    val playlistIdForAddSongCommand: String = properties.getProperty("playlist_id_for_add_song_command")
}