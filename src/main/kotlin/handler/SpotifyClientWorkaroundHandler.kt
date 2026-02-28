package handler

import com.adamratzman.spotify.models.*
import com.adamratzman.spotify.utils.ExternalUrls
import com.adamratzman.spotify.utils.Market
import com.adamratzman.spotify.utils.getExternalUrls
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

// TODO Remove when fixed in Spotify-Kotlin-API https://github.com/adamint/spotify-web-api-kotlin/issues/343
class SpotifyClientWorkaroundHandler() {
    private val json = Json { ignoreUnknownKeys = true }

    private val baseUrlToSpotifyApi = "https://api.spotify.com/v1/"
    private val playlistsEndpoint = baseUrlToSpotifyApi + "playlists/"
    private val playerEndpoint = baseUrlToSpotifyApi + "me/player"

    private val addItemsToPlaylistEndpoint = "/items"
    private val getPlaylistItemsEndpoint = "/items"
    private val getTrackEndpoint = "/tracks"
    private val searchEndpoint = "/search"
    private val currentlyPlayingEndpoint = "/currently-playing"
    private val queueEndpoint = "/queue"

    suspend fun addItemsToPlaylist(playlistId: String, trackUri: PlayableUri): Boolean {
        spotifyDummyCallToRefreshAccessToken()
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

    suspend fun getPlaylistItems(playlistId: String, offset: Int, limit: Int): PagingObject<WorkaroundPlaylistTrack>? {
        spotifyDummyCallToRefreshAccessToken()
        val endpoint = "$playlistsEndpoint$playlistId$getPlaylistItemsEndpoint"
        val endpointWithOptions = "$endpoint?offset=$offset&limit=$limit"

        val response = httpClient.get(endpointWithOptions) {
            header("Authorization", "Bearer ${spotifyClient.token.accessToken}")
        }

        return if(response.status != HttpStatusCode.OK) {
            logHttpError(response, endpoint)
            null
        } else {
            json.decodeFromString<PagingObject<WorkaroundPlaylistTrack>>(response.bodyAsText())
        }
    }


    suspend fun getTrack(trackId: String): WorkaroundTrack? {
        spotifyDummyCallToRefreshAccessToken()
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
        spotifyDummyCallToRefreshAccessToken()
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


    suspend fun getCurrentlyPlaying(): WorkaroundCurrentlyPlayingObject? {
        spotifyDummyCallToRefreshAccessToken()
        val endpoint = "$playerEndpoint$currentlyPlayingEndpoint"
        val additionalTypes = URLEncoder.encode(listOf(
            CurrentlyPlayingType.Track,
            CurrentlyPlayingType.Episode
        ).joinToString(",") { it.identifier }, "UTF-8")

        val endpointWithOptions = "$endpoint?market=${Market.DE}&additional_types=$additionalTypes"

        val response = httpClient.get(endpointWithOptions) {
            header("Authorization", "Bearer ${spotifyClient.token.accessToken}")
        }

        return if(response.status == HttpStatusCode.OK || response.status == HttpStatusCode.NoContent) {
            json.decodeFromString<WorkaroundCurrentlyPlayingObject>(response.bodyAsText())
        } else {
            logHttpError(response, endpoint)
            null
        }
    }

    suspend fun getUsersQueue(): WorkaroundCurrentUserQueue? {
        spotifyDummyCallToRefreshAccessToken()
        val endpoint = "$playerEndpoint$queueEndpoint"

        val response = httpClient.get(endpoint) {
            header("Authorization", "Bearer ${spotifyClient.token.accessToken}")
        }

        return if(response.status != HttpStatusCode.OK) {
            logHttpError(response, endpoint)
            null
        } else {
            json.decodeFromString<WorkaroundCurrentUserQueue>(response.bodyAsText())
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

    private suspend fun spotifyDummyCallToRefreshAccessToken() {
        try {
            spotifyClient.player.getDevices()
        } catch (e: Exception) {
            logger.error("Error while accessing devices-endpoint in spotifyDummyCallToRefreshAccessToken: ${e.stackTrace}")
        }
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
    val href: String,
    val id: String,
    val uri: PlayableUri,

    val album: WorkaroundSimpleAlbum,
    val artists: List<SimpleArtist>,
    @SerialName("is_playable") val isPlayable: Boolean = true,
    @SerialName("disc_number") val discNumber: Int,
    @SerialName("duration_ms") val durationMs: Int,
    val explicit: Boolean,
    val name: String,
    @SerialName("preview_url") val previewUrl: String? = null,
    @SerialName("track_number") val trackNumber: Int,
    val type: String,
    @SerialName("is_local") val isLocal: Boolean? = null,
    val restrictions: Restrictions? = null,

    val episode: Boolean? = null,
    val track: Boolean? = null
) {
    val length: Int get() = durationMs
    val externalUrls: ExternalUrls get() = getExternalUrls(externalUrlsString)
}

@Serializable
data class WorkaroundPlaylistTrack(
    @SerialName("primary_color") val primaryColor: String? = null,
    @SerialName("added_at") val addedAt: String? = null,
    @SerialName("added_by") val addedBy: SpotifyPublicUser? = null,
    @SerialName("is_local") val isLocal: Boolean? = null,
    val item: WorkaroundTrack? = null,
    @SerialName("video_thumbnail") val videoThumbnail: VideoThumbnail? = null
)


@Serializable
data class WorkaroundSpotifySearchResult(
    val tracks: PagingObject<WorkaroundTrack>? = null,
)

@Serializable
data class WorkaroundCurrentlyPlayingObject(
    val context: SpotifyContext? = null,
    val timestamp: Long,
    @SerialName("progress_ms") val progressMs: Int? = null,
    @SerialName("is_playing") val isPlaying: Boolean,
    @SerialName("item")
    val item: WorkaroundTrack? = null,
    val actions: PlaybackActions
)

@Serializable
data class WorkaroundCurrentUserQueue(
    @SerialName("queue") val queue: List<WorkaroundTrack>
)
