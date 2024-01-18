package commands

import config.TwitchBotConfig
import handleSongRequestQuery
import handler.Command
import logger
import sendMessageToTwitchChatAndLogIt
import ui.isSongRequestEnabled
import ui.isSongRequestEnabledAsCommand
import kotlin.time.Duration.Companion.seconds

val songRequestCommand = Command(
    names = listOf("songrequest", "sr"),
    handler = {arguments ->
        val query = arguments.joinToString(" ").trim()

        if(!isSongRequestEnabledAsCommand.value || !isSongRequestEnabled.value) {
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
            addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDown
            addedUserCoolDown = TwitchBotConfig.defaultUserCoolDown
        } else {
            addedCommandCoolDown = 5.seconds
            addedUserCoolDown = 5.seconds
        }
    }
)