package redeems

import config.TwitchBotConfig
import handleCommandSanityChecksWithoutSecurityLevel
import handleSongRequestQuery
import handler.Redeem
import isSongRequestRedeemActive
import logger
import spotifyClientWorkaroundHandler

val songRequestRedeem: Redeem = Redeem(
    id = TwitchBotConfig.songRequestRedeemId,
    handler = { query ->

        if(!handleCommandSanityChecksWithoutSecurityLevel(
                commandName = "songRequestRedeem",
                isCommandEnabledFlag = isSongRequestRedeemActive(),
                userName = redeemEvent.userName,
                userID = redeemEvent.userId,
                twitchClient = twitchClient
            )) {
            return@Redeem
        }

        logger.info("query: $query")

        // TODO Remove when fixed in Spotify-Kotlin-API
        //val queueBefore = spotifyClient.player.getUserQueue().queue
        val queueBefore = spotifyClientWorkaroundHandler.getUsersQueue()?.queue
        val success = handleSongRequestQuery(twitchClient.chat, query)
        if (success) {
            requestedByQueueHandler.addEntryToRequestedByQueue(
                queueBefore,
                redeemEvent.userName
            )
        }
    }
)