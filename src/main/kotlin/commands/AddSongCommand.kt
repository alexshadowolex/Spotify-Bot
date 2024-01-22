package commands

import addSongToPlaylist
import areUsersPermissionsEligibleForAddSongCommand
import config.TwitchBotConfig
import getCurrentSpotifySong
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

        if(!areUsersPermissionsEligibleForAddSongCommand(messageEvent.permissions)) {
            logger.info("User ${messageEvent.user.name} tried using addSongCommand but was not eligible. " +
                    "Current security setting: ${addSongCommandSecurityLevel.value}"
            )

            sendMessageToTwitchChatAndLogIt(chat, "You are not eligible to use that command!")
            return@Command
        }

        val currentSong = getCurrentSpotifySong()
        val success = if (currentSong != null) {
            addSongToPlaylist(currentSong)
        } else {
            false
        }

        val message = if(success) {
            "Successfully added song \"${currentSong!!.name}\" to the playlist!"
        } else {
            "Something went wrong when adding the song to the playlist."
        }

        sendMessageToTwitchChatAndLogIt(chat, message)

        addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDown
    }
)