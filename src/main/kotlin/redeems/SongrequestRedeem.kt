package redeems

import config.TwitchBotConfig
import handleSongRequestQuery
import handler.Redeem
import isSongRequestRedeemEnabled
import logger

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