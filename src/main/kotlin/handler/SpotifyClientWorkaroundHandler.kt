package handler

import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.CoreObject
import com.adamratzman.spotify.models.ExternalId
import com.adamratzman.spotify.models.LinkedTrack
import com.adamratzman.spotify.models.NeedsApi
import com.adamratzman.spotify.models.NullablePagingObject
import com.adamratzman.spotify.models.PagingObject
import com.adamratzman.spotify.models.Playable
import com.adamratzman.spotify.models.PlayableUri
import com.adamratzman.spotify.models.PlaylistTrack
import com.adamratzman.spotify.models.RelinkingAvailableResponse
import com.adamratzman.spotify.models.Restrictions
import com.adamratzman.spotify.models.SimpleAlbum
import com.adamratzman.spotify.models.SimpleArtist
import com.adamratzman.spotify.models.SimpleEpisode
import com.adamratzman.spotify.models.SimplePlaylist
import com.adamratzman.spotify.models.SimpleShow
import com.adamratzman.spotify.models.SpotifyImage
import com.adamratzman.spotify.models.SpotifySearchResult
import com.adamratzman.spotify.models.Track
import com.adamratzman.spotify.utils.Market
import httpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import logger
import spotifyClient
import java.net.URLEncoder
import kotlin.collections.plus

// TODO Remove when fixed in Spotify-Kotlin-API https://github.com/adamint/spotify-web-api-kotlin/issues/343
class SpotifyClientWorkaroundHandler() {
    private val json = Json { ignoreUnknownKeys = true }

    private val baseUrlToSpotifyApi = "https://api.spotify.com/v1/"
    private val playlistsEndpoint = baseUrlToSpotifyApi + "playlists/"
    private val addItemsToPlaylistEndpoint = "/items"
    private val getPlaylistItemsEndpoint = "/items"
    private val getTrackEndpoint = "/tracks"
    private val searchEndpoint = "/search"

    suspend fun addItemsToPlaylist(playlistId: String, trackUri: PlayableUri): Boolean {
        val endpoint = "$playlistsEndpoint$playlistId$addItemsToPlaylistEndpoint"
        val response = httpClient.post(endpoint) {
            header("Authorization", "Bearer ${spotifyClient.token.accessToken}")
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
        val endpoint = "$playlistsEndpoint$playlistId$getPlaylistItemsEndpoint"
        val endpointWithOptions = "$endpoint?offset=$offset&limit=$limit"

        val response = httpClient.get(endpointWithOptions) {
            header("Authorization", "Bearer ${spotifyClient.token.accessToken}")
        }

        return if(response.status != HttpStatusCode.OK) {
            logHttpError(response, endpoint)
            null
        } else {
            json.decodeFromString<PagingObject<PlaylistTrack>>(response.bodyAsText())
        }
    }


    suspend fun getTrack(trackId: String): WorkaroundTrack? {
        val endpoint = "$baseUrlToSpotifyApi$getTrackEndpoint/$trackId"
        val endpointWithOptions = "$endpoint?market=${Market.DE}"

        val response = httpClient.get(endpointWithOptions) {
            header("Authorization", "Bearer ${spotifyClient.token.accessToken}")
        }

        return if(response.status != HttpStatusCode.OK) {
            logHttpError(response, endpoint)
            null
        } else {
            json.decodeFromString<WorkaroundTrack>(response.bodyAsText())
        }
    }


    suspend fun search(query: String): WorkaroundSpotifySearchResult? {
        val queryUrlEncoded = URLEncoder.encode(query, "UTF-8")
        val typeEncoded = "album%2Cartist%2Ctrack"

        val endpoint = "$baseUrlToSpotifyApi$searchEndpoint"
        val endpointWithOptions = "$endpoint?q=$queryUrlEncoded&type=$typeEncoded&market=${Market.DE}&limit=1"

        val response = httpClient.get(endpointWithOptions) {
            header("Authorization", "Bearer ${spotifyClient.token.accessToken}")
        }

        return if(response.status != HttpStatusCode.OK) {
            logHttpError(response, endpoint)
            null
        } else {
            json.decodeFromString<WorkaroundSpotifySearchResult>(response.bodyAsText())
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

@Serializable
data class WorkaroundSimpleAlbum(
    val images: List<SpotifyImage>? = null,
)

@Serializable
data class WorkaroundTrack(
    @SerialName("external_urls") val externalUrlsString: Map<String, String>,
    override val href: String,
    override val id: String,
    override val uri: PlayableUri,

    val album: WorkaroundSimpleAlbum,
    val artists: List<SimpleArtist>,
    @SerialName("is_playable") val isPlayable: Boolean = true,
    @SerialName("disc_number") val discNumber: Int,
    @SerialName("duration_ms") val durationMs: Int,
    val explicit: Boolean,
    val name: String,
    val popularity: Double,
    @SerialName("preview_url") val previewUrl: String? = null,
    @SerialName("track_number") val trackNumber: Int,
    override val type: String,
    @SerialName("is_local") val isLocal: Boolean? = null,
    val restrictions: Restrictions? = null,

    val episode: Boolean? = null,
    val track: Boolean? = null
) : Playable {
    val length: Int get() = durationMs
}

@Serializable
data class WorkaroundSpotifySearchResult(
    val tracks: PagingObject<WorkaroundTrack>? = null,
)
