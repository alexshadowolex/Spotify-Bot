package commands

import addQuotationMarks
import config.BotConfig
import config.TwitchBotConfig
import createSongString
import handleCommandSanityChecksWithSecurityLevel
import handler.Command
import isUserEligibleForRemoveSongFromQueueCommand
import logger
import sendMessageToTwitchChatAndLogIt
import kotlin.time.Duration.Companion.seconds

val removeSongFromQueueCommand: Command = Command(
    commandDisplayName = "Remove Song From Queue",
    names = listOf("removesongfromqueue", "rsfq", "remove", "removesong", "rs"),
    handler = { input ->
        val inputString = input.joinToString(" ")

        if(!handleCommandSanityChecksWithSecurityLevel(
                commandName = "removeSongFromQueueCommand",
                isCommandEnabledFlag= BotConfig.isRemoveSongFromQueueCommandEnabled,
                messageEvent = messageEvent,
                twitchClient = twitchClient,
                securityCheckFunction = ::isUserEligibleForRemoveSongFromQueueCommand,
                securityLevel = BotConfig.removeSongFromQueueCommandSecurityLevel,
            )) {
            return@Command
        }

        if (inputString.isEmpty()) {
            sendMessageToTwitchChatAndLogIt(twitchClient.chat, "No input provided.")
            addedUserCoolDown = 5.seconds
            return@Command
        }


        val foundTrack = removeSongFromQueueHandler.addSongToSetMarkedForSkipping(inputString)
        val message = if(foundTrack != null) {
            addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDownSeconds
            "Removed song ${createSongString(foundTrack.name, foundTrack.artists)} from queue"
        } else {
            addedUserCoolDown = 5.seconds
            "Something went wrong with removing that song from queue. Try again."
        }

        sendMessageToTwitchChatAndLogIt(twitchClient.chat, message)
    }
)