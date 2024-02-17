package commands

import areUsersPermissionsEligibleForAddSongCommand
import config.TwitchBotConfig
import getCurrentSpotifySong
import handleAddSongCommandFunctionality
import handler.Command
import logger
import sendMessageToTwitchChatAndLogIt
import ui.addSongCommandSecurityLevel
import ui.isAddSongCommandEnabled

val addSongCommand: Command = Command(
    names = listOf("addsong", "as", "add"),
    handler = {
        if(!isAddSongCommandEnabled.value) {
            logger.info("AddSongCommand disabled. Aborting execution")
            return@Command
        }

        if(!areUsersPermissionsEligibleForAddSongCommand(messageEvent.permissions, messageEvent.user.name)) {
            logger.info("User ${messageEvent.user.name} tried using addSongCommand but was not eligible. " +
                    "Current security setting: ${addSongCommandSecurityLevel.value}"
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

        addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDown
    }
)