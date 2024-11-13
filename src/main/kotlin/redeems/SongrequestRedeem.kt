package redeems

import config.TwitchBotConfig
import handleCommandSanityChecksWithoutSecurityLevel
import handleSongRequestQuery
import handler.Redeem
import isSongRequestRedeemActive
import logger

val songRequestRedeem: Redeem = Redeem(
    id = TwitchBotConfig.songRequestRedeemId,
    handler = { query ->

        if(!handleCommandSanityChecksWithoutSecurityLevel(
                commandName = "songRequestRedeem",
                isCommandEnabledFlag = isSongRequestRedeemActive(),
                userName = redeemEvent.redemption.user.displayName,
                userID = redeemEvent.redemption.user.id,
                twitchClient = twitchClient
            )) {
            return@Redeem
        }

        logger.info("query: $query")

        handleSongRequestQuery(twitchClient.chat, query)
    }
)