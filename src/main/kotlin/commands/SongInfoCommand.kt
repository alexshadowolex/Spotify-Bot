package commands

import config.BotConfig
import config.TwitchBotConfig
import createSongString
import getCurrentSpotifySong
import handler.Command
import handleCommandSanityChecksWithoutSecurityLevel
import isSpotifyPlaying
import sendMessageToTwitchChatAndLogIt

val songInfoCommand: Command = Command(
    commandDisplayName = "Song Info",
    names = listOf("songinfo", "si"),
    handler = {

        if(!handleCommandSanityChecksWithoutSecurityLevel(
                commandName = "songInfoCommand",
                isCommandEnabledFlag = BotConfig.isSongInfoCommandEnabled,
                userName = messageEvent.user.name,
                userID = messageEvent.user.id,
                twitchClient = twitchClient
        )) {
            return@Command
        }

        val currentSong = getCurrentSpotifySong()
        val message = if(currentSong == null || isSpotifyPlaying() == false) {
            "No Song playing right now!"
        } else {
            createSongString(currentSong.name, currentSong.artists) + " --> ${currentSong.externalUrls.spotify}"
        }

        sendMessageToTwitchChatAndLogIt(twitchClient.chat, message)

        addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDownSeconds
    }
)