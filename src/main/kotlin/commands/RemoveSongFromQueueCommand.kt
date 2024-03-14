package commands

import config.BotConfig
import config.TwitchBotConfig
import handler.Command
import isUserEligibleForRemoveSongFromQueueCommand
import logger
import sendMessageToTwitchChatAndLogIt
import kotlin.time.Duration.Companion.seconds

val removeSongFromQueueCommand: Command = Command(
    names = listOf("removesongfromqueue", "rsfq", "remove", "removesong", "rs"),
    handler = { input ->
        val inputString = input.joinToString(" ")
        if(!BotConfig.isRemoveSongFromQueueCommandEnabled) {
            logger.info("removeSongFromQueueCommand disabled. Aborting execution")
            return@Command
        }

        if(!isUserEligibleForRemoveSongFromQueueCommand(messageEvent.permissions, messageEvent.user.name)) {
            logger.info("User ${messageEvent.user.name} tried using removeSongFromQueueCommand but was not eligible. " +
                    "Current security setting: ${BotConfig.removeSongFromQueueCommandSecurityLevel}"
            )

            sendMessageToTwitchChatAndLogIt(chat, "You are not eligible to use that command!")
            return@Command
        }

        if (inputString.isEmpty()) {
            sendMessageToTwitchChatAndLogIt(chat, "No input provided.")
            addedUserCoolDown = 5.seconds
            return@Command
        }


        val success = removeSongFromQueueHandler.addSongToSetMarkedForSkipping(inputString)
        val message = if(success) {
            addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDownSeconds
            "Removed song ${inputString.substringBefore(" by")} from queue"
        } else {
            addedUserCoolDown = 5.seconds
            "Something went wrong with removing that song from queue. Try again."
        }

        sendMessageToTwitchChatAndLogIt(chat, message)
    }
)