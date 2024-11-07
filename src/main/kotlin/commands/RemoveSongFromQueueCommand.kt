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
                permissions = messageEvent.permissions,
                userName = messageEvent.user.name,
                securityCheckFunction = ::isUserEligibleForRemoveSongFromQueueCommand,
                securityLevel = BotConfig.removeSongFromQueueCommandSecurityLevel,
                chat = chat
            )) {
            return@Command
        }

        if (inputString.isEmpty()) {
            sendMessageToTwitchChatAndLogIt(chat, "No input provided.")
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

        sendMessageToTwitchChatAndLogIt(chat, message)
    }
)