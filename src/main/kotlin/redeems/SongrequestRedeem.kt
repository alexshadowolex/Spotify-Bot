package redeems

import config.TwitchBotConfig
import Redeem
import com.adamratzman.spotify.endpoints.pub.SearchApi
import com.adamratzman.spotify.utils.Market
import httpClient
import io.ktor.client.request.*
import io.ktor.http.*
import logger
import spotifyClient

val songRequestRedeem: Redeem = Redeem(
    id = TwitchBotConfig.songRequestRedeemId,
    handler = { query ->
        logger.info("Used SongRequestRedeem.")
        logger.error(query)

        val result = try {
            Url(query).takeIf { it.host == "open.spotify.com" && it.encodedPath.startsWith("/track/") }?.let {
                val songId = it.encodedPath.substringAfter("/track/")
                logger.info("Song ID from link: $songId")
                spotifyClient.tracks.getTrack(
                    track = songId,
                    market = Market.DE
                )
            } ?: run {
                spotifyClient.search.search(
                    query = query,
                    searchTypes = arrayOf(
                        SearchApi.SearchType.ARTIST,
                        SearchApi.SearchType.ALBUM,
                        SearchApi.SearchType.TRACK
                    ),
                    market = Market.DE
                ).tracks?.firstOrNull()
            } ?: return@Redeem
        } catch (e: Exception) {
            logger.error("Error while searching for track:", e)
            return@Redeem
        }

        logger.info("Result after search: $result")

        try {
            httpClient.post("https://api.spotify.com/v1/me/player/queue") {
                header("Authorization", "Bearer ${spotifyClient.token.accessToken}")

                url {
                    parameters.append("uri", result.uri.uri)
                }
            }

            logger.info("Result URI: ${result.uri.uri}")
        } catch (e: Exception) {
            logger.error("Spotify is probably not set up.", e)
            return@Redeem
        }
    }
)