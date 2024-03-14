package commands

import config.BotConfig
import config.TwitchBotConfig
import getCurrentSpotifySong
import handleAddSongCommandFunctionality
import handler.Command
import isUserEligibleForAddSongCommand
import logger
import sendMessageToTwitchChatAndLogIt

val addSongCommand: Command = Command(
    names = listOf("addsong", "as", "add"),
    handler = {
        if(!BotConfig.isAddSongCommandEnabled) {
            logger.info("AddSongCommand disabled. Aborting execution")
            return@Command
        }

        if(!isUserEligibleForAddSongCommand(messageEvent.permissions, messageEvent.user.name)) {
            logger.info("User ${messageEvent.user.name} tried using addSongCommand but was not eligible. " +
                    "Current security setting: ${BotConfig.addSongCommandSecurityLevel}"
            )

            sendMessageToTwitchChatAndLogIt(chat, "You are not eligible to use that command!")
            return@Command
        }

        val currentSong = getCurrentSpotifySong()
        val message = if(currentSong == null) {
            "Something went wrong when adding the song to the playlist."
        } else {
            handleAddSongCommandFunctionality(currentSong)
        }

        sendMessageToTwitchChatAndLogIt(chat, message)

        addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDownSeconds
    }
)