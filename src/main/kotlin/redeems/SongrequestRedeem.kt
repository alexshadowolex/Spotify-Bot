package redeems

import config.BotConfig
import config.TwitchBotConfig
import handleSongRequestQuery
import handler.Redeem
import isSongRequestEnabledAsRedeem
import logger

val songRequestRedeem: Redeem = Redeem(
    id = TwitchBotConfig.songRequestRedeemId,
    handler = { query ->
        if(!isSongRequestEnabledAsRedeem() || !BotConfig.isSongRequestEnabled) {
            logger.info("SongRequestRedeem disabled. Aborting execution")
            return@Redeem
        }

        logger.info("query: $query")

        handleSongRequestQuery(chat, query)
    }
)