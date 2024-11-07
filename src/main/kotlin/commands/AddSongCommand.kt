package commands

import config.BotConfig
import config.SpotifyConfig
import config.TwitchBotConfig
import getCurrentSpotifySong
import handleAddSongCommandFunctionality
import handleCommandSanityChecksWithSecurityLevel
import handler.Command
import isPlaylistIdValid
import isUserEligibleForAddSongCommand
import logger
import sendMessageToTwitchChatAndLogIt

val addSongCommand: Command = Command(
    commandDisplayName = "Add Song",
    names = listOf("addsong", "as", "add"),
    handler = {

        if(!handleCommandSanityChecksWithSecurityLevel(
                commandName = "addSongCommand",
                isCommandEnabledFlag= BotConfig.isAddSongCommandEnabled,
                permissions = messageEvent.permissions,
                userName = messageEvent.user.name,
                securityCheckFunction = ::isUserEligibleForAddSongCommand,
                securityLevel = BotConfig.addSongCommandSecurityLevel,
                chat = chat
            )) {
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