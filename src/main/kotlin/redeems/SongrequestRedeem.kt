package redeems

import handler.Redeem
import com.adamratzman.spotify.endpoints.pub.SearchApi
import com.adamratzman.spotify.utils.Market
import httpClient
import io.ktor.client.request.*
import io.ktor.http.*
import logger
import spotifyClient
import config.TwitchBotConfig
import handleSongRequestQuery
import isSongRequestRedeemEnabled

val songRequestRedeem: Redeem = Redeem(
    id = TwitchBotConfig.songRequestRedeemId,
    handler = { query ->
        if(!isSongRequestRedeemEnabled) {
            logger.info("SongRequestRedeem disabled. Aborting execution")
            return@Redeem
        }
        logger.info("Used SongRequestRedeem.")
        logger.info("query: $query")

        handleSongRequestQuery(chat, query)
    }
)