package handler

import com.adamratzman.spotify.models.PagingObject
import com.adamratzman.spotify.models.PlayableUri
import com.adamratzman.spotify.models.PlaylistTrack
import httpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logger
import spotifyClient

// TODO Remove when fixed in Spotify-Kotlin-API https://github.com/adamint/spotify-web-api-kotlin/issues/343
class SpotifyClientWorkaroundHandler() {
    private val accessToken = spotifyClient.token.accessToken

    private val baseUrlToSpotifyApi = "https://api.spotify.com/v1/"
    private val playlistsEndpoint = baseUrlToSpotifyApi + "playlists/"
    private val addItemsToPlaylistEndpoint = "/items"
    private val getPlaylistItemsEndpoint = "/items"

    suspend fun addItemsToPlaylist(playlistId: String, trackUri: PlayableUri): Boolean {
        val endpoint = "$playlistsEndpoint$playlistId$addItemsToPlaylistEndpoint"
        val response = httpClient.post(endpoint) {
            header("Authorization", "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(AddItemsToPlaylistBody(listOf(trackUri)))
        }

        return if(response.status == HttpStatusCode.Created) {
            true
        } else {
            logHttpError(response, endpoint)
            false
        }
    }

    suspend fun getPlaylistItems(playlistId: String, offset: Int, limit: Int): PagingObject<PlaylistTrack>? {
        val json = Json { ignoreUnknownKeys = true }
        val endpoint = "$playlistsEndpoint$playlistId$getPlaylistItemsEndpoint"
        val endpointWithOptions = "$endpoint?offset=$offset&limit=$limit"

        val response = httpClient.get(endpointWithOptions) {
            header("Authorization", "Bearer $accessToken")
        }

        return if(response.status != HttpStatusCode.OK) {
            logHttpError(response, endpoint)
            null
        } else {
            json.decodeFromString<PagingObject<PlaylistTrack>>(response.bodyAsText())
        }
    }

    private suspend fun logHttpError(response: HttpResponse, endpoint: String) {
        logger.error(
            "Error while sending Request to endpoint $endpoint\n" +
            "Request:\n${response.request.method}\n${response.request.content}\n" +
            "${response.status.value}\n" +
            response.bodyAsText()
        )
    }
}

@Serializable
private data class AddItemsToPlaylistBody(
    val uris: List<PlayableUri>
)