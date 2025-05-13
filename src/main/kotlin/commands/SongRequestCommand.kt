package commands

import config.TwitchBotConfig
import handleCommandSanityChecksWithoutSecurityLevel
import handleSongRequestQuery
import handler.Command
import isSongRequestCommandActive
import logger
import sendMessageToTwitchChatAndLogIt
import spotifyClient
import kotlin.time.Duration.Companion.seconds

val songRequestCommand = Command(
    commandDisplayName = "Song Request",
    names = listOf("songrequest", "sr"),
    handler = { arguments ->
        val query = arguments.joinToString(" ").trim()

        if(!handleCommandSanityChecksWithoutSecurityLevel(
                commandName = "songRequestCommand",
                isCommandEnabledFlag = isSongRequestCommandActive(),
                userName = messageEvent.user.name,
                userID = messageEvent.user.id,
                twitchClient = twitchClient
            )) {
            return@Command
        }

        if (query.isBlank()) {
            sendMessageToTwitchChatAndLogIt(twitchClient.chat, "No song given.")
            return@Command
        }

        logger.info("query: $query")

        // The queue has to be saved before successfully adding the song to the queue for the RequestedBy-handler!
        val queueBefore = spotifyClient.player.getUserQueue().queue
        val success = handleSongRequestQuery(twitchClient.chat, query)
        if (success) {
            addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDownSeconds
            addedUserCoolDown = TwitchBotConfig.defaultUserCoolDownSeconds

            requestedByQueueHandler.addEntryToRequestedByQueue(queueBefore, messageEvent.user.name)
        } else {
            addedCommandCoolDown = 5.seconds
            addedUserCoolDown = 5.seconds
        }
    }
)