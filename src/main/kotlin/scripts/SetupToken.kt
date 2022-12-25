package scripts

import config.TwitchBotConfig
import com.adamratzman.spotify.SpotifyScope
import com.adamratzman.spotify.getSpotifyAuthorizationUrl
import com.adamratzman.spotify.spotifyClientApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.io.File
import java.net.URI

suspend fun main() {
    val redirectUri = "https://www.example.com"

    @Suppress("BlockingMethodInNonBlockingContext")
    Desktop.getDesktop().browse(
        URI.create(
            getSpotifyAuthorizationUrl(
                scopes = SpotifyScope.values(),
                clientId = TwitchBotConfig.spotifyClientId,
                redirectUri = redirectUri
            )
        )
    )

    print("Spotify code: ")
    val code = readln()

    val api = spotifyClientApi {
        credentials {
            clientId = TwitchBotConfig.spotifyClientId
            clientSecret = TwitchBotConfig.spotifyClientSecret
            this.redirectUri = redirectUri
        }

        authorization {
            authorizationCode = code
        }
    }.build()

    println("Token: " + Json.encodeToString(api.token))
    File("data/spotifytoken.json").writeText(Json.encodeToString(api.token))
}