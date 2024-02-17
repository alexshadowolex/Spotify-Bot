package commands

import config.TwitchBotConfig
import handler.Command
import isUserEligibleForRemoveSongFromQueueCommand
import logger
import sendMessageToTwitchChatAndLogIt
import ui.isRemoveSongFromQueueCommandEnabled
import ui.removeSongFromQueueCommandSecurityLevel

val removeSongFromQueueCommand: Command = Command(
    names = listOf("removesongfromqueue", "rsfq", "remove", "removesong", "rs"),
    handler = {
        if(!isRemoveSongFromQueueCommandEnabled.value) {
            logger.info("removeSongFromQueueCommand disabled. Aborting execution")
            return@Command
        }


        if(!isUserEligibleForRemoveSongFromQueueCommand(messageEvent.permissions, messageEvent.user.name)) {
            logger.info("User ${messageEvent.user.name} tried using removeSongFromQueueCommand but was not eligible. " +
                    "Current security setting: ${removeSongFromQueueCommandSecurityLevel.value}"
            )

            sendMessageToTwitchChatAndLogIt(chat, "You are not eligible to use that command!")
            return@Command
        }

        sendMessageToTwitchChatAndLogIt(chat, "Marked song <song> for skipping (aka. removed it)")

        addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDown
    }
)