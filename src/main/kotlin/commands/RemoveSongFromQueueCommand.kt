package commands

import config.TwitchBotConfig
import createSongString
import currentSongString
import handler.Command
import isUserEligibleForRemoveSongFromQueueCommand
import logger
import sendMessageToTwitchChatAndLogIt
import ui.isRemoveSongFromQueueCommandEnabled
import ui.removeSongFromQueueCommandSecurityLevel
import kotlin.time.Duration.Companion.seconds

val removeSongFromQueueCommand: Command = Command(
    names = listOf("removesongfromqueue", "rsfq", "remove", "removesong", "rs"),
    handler = { input ->
        val inputString = input.joinToString(" ")
        if(!isRemoveSongFromQueueCommandEnabled.value) {
            logger.info("removeSongFromQueueCommand disabled. Aborting execution")
            return@Command
        }

        if (inputString.isEmpty()) {
            sendMessageToTwitchChatAndLogIt(chat, "No input provided.")
            addedUserCoolDown = 5.seconds
            return@Command
        }

        if(!isUserEligibleForRemoveSongFromQueueCommand(messageEvent.permissions, messageEvent.user.name)) {
            logger.info("User ${messageEvent.user.name} tried using removeSongFromQueueCommand but was not eligible. " +
                    "Current security setting: ${removeSongFromQueueCommandSecurityLevel.value}"
            )

            sendMessageToTwitchChatAndLogIt(chat, "You are not eligible to use that command!")
            return@Command
        }

        val success = removeSongFromQueueHandler.addSongToSetMarkedForSkipping(inputString)
        val message = if(success) {
            addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDown
            "Removed song ${inputString.substringBefore(" by")} from queue"
        } else {
            addedUserCoolDown = 5.seconds
            "Something went wrong with removing that song from queue. Try again."
        }

        sendMessageToTwitchChatAndLogIt(chat, message)
    }
)