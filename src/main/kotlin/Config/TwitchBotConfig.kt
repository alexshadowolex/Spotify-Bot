package Config

import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.seconds

object TwitchBotConfig {
    private val properties = Properties().apply {
        load(File("data/twitchBotconfig.properties").inputStream())
    }

    val channel: String = properties.getProperty("channel")
    val spotifyClientId: String = properties.getProperty("spotify_client_id")
    val spotifyClientSecret: String = properties.getProperty("spotify_client_secret")
    val songRequestRedeemId: String = properties.getProperty("song_request_redeem_id")
}