package handler

import com.adamratzman.spotify.models.PlayableUri
import httpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import logger
import spotifyClient

// TODO Remove when fixed in Spotify-Kotlin-API https://github.com/adamint/spotify-web-api-kotlin/issues/343
class SpotifyClientWorkaroundHandler() {
    private val accessToken = spotifyClient.token.accessToken

    private val baseUrlToSpotifyApi = "https://api.spotify.com/v1/"
    private val playlistsEndpoint = baseUrlToSpotifyApi + "playlists/"
    private val addItemsToPlaylistEndpoint = "/items"

    suspend fun addItemsToPlaylist(playlistId: String, trackUri: PlayableUri): Boolean {
        val endpoint = "$playlistsEndpoint$playlistId$addItemsToPlaylistEndpoint"
        val response = httpClient.post(endpoint) {
            header("Authorization", "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(AddItemsToPlaylistBody(listOf(trackUri)))
        }

        return if(response.status != HttpStatusCode.Created) {
            true
        } else {
            logger.error(
                "Error while sending POST-Request to endpoint $endpoint\n" +
                "${response.status.value}\n" +
                response.bodyAsText()
            )
            false
        }
    }
}

@Serializable
private data class AddItemsToPlaylistBody(
    val uris: List<PlayableUri>
)