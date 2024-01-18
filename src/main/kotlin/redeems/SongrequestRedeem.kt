package redeems

import config.TwitchBotConfig
import handleSongRequestQuery
import handler.Redeem
import isSongRequestEnabledAsRedeem
import logger
import ui.isSongRequestEnabled

val songRequestRedeem: Redeem = Redeem(
    id = TwitchBotConfig.songRequestRedeemId,
    handler = { query ->
        if(!isSongRequestEnabledAsRedeem() || !isSongRequestEnabled.value) {
            logger.info("SongRequestRedeem disabled. Aborting execution")
            return@Redeem
        }

        logger.info("query: $query")

        handleSongRequestQuery(chat, query)
    }
)