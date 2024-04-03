package commands

import config.BotConfig
import config.SpotifyConfig
import config.TwitchBotConfig
import getCurrentSpotifySong
import handleAddSongCommandFunctionality
import handler.Command
import isPlaylistIdValid
import isUserEligibleForAddSongCommand
import logger
import sendMessageToTwitchChatAndLogIt

val addSongCommand: Command = Command(
    commandDisplayName = "Add Song",
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

        if(!isPlaylistIdValid(SpotifyConfig.playlistIdForAddSongCommand)) {
            sendMessageToTwitchChatAndLogIt(chat, "Playlist ID seems invalid. Correct it or try again!")
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