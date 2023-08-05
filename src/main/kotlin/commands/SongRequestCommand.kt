package commands

import config.TwitchBotConfig
import handleSongRequestQuery
import handler.Command
import isSongRequestCommandEnabled
import logger
import kotlin.time.Duration.Companion.seconds

val songRequestCommand = Command(
    names = listOf("songrequest", "sr"),
    handler = {arguments ->
        if(!isSongRequestCommandEnabled) {
            logger.info("SongRequestCommand disabled. Aborting execution")
            return@Command
        }

        if (arguments.isEmpty()) {
            chat.sendMessage(TwitchBotConfig.channel, "No song given.")
            return@Command
        }

        val query = arguments.joinToString(" ")
        logger.info("Used SongRequestRedeem.")
        logger.info("query: $query")

        val success = handleSongRequestQuery(chat, query)
        if (success) {
            addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDown
            addedUserCoolDown = TwitchBotConfig.defaultUserCoolDown
        } else {
            addedCommandCoolDown = 5.seconds
            addedUserCoolDown = 5.seconds
        }
    }
)