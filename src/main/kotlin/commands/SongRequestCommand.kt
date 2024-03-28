package commands

import config.BotConfig
import config.TwitchBotConfig
import handleSongRequestQuery
import handler.Command
import logger
import sendMessageToTwitchChatAndLogIt
import kotlin.time.Duration.Companion.seconds

val songRequestCommand = Command(
    commandDisplayName = "Song Request",
    names = listOf("songrequest", "sr"),
    handler = {arguments ->
        val query = arguments.joinToString(" ").trim()

        if(!BotConfig.isSongRequestCommandEnabled || !BotConfig.isSongRequestEnabled) {
            logger.info("SongRequestCommand disabled. Aborting execution")
            return@Command
        }

        if (query.isBlank()) {
            sendMessageToTwitchChatAndLogIt(chat, "No song given.")
            return@Command
        }

        logger.info("query: $query")

        val success = handleSongRequestQuery(chat, query)
        if (success) {
            addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDownSeconds
            addedUserCoolDown = TwitchBotConfig.defaultUserCoolDownSeconds
        } else {
            addedCommandCoolDown = 5.seconds
            addedUserCoolDown = 5.seconds
        }
    }
)