package commands

import config.TwitchBotConfig
import createSongString
import getCurrentSpotifySong
import handler.Command
import isSpotifyPlaying
import logger
import sendMessageToTwitchChatAndLogIt
import ui.isSongInfoCommandEnabled

val songInfoCommand: Command = Command(
    names = listOf("songinfo", "si"),
    handler = {
        if(!isSongInfoCommandEnabled.value) {
            logger.info("SongInfoCommand disabled. Aborting execution")
            return@Command
        }

        val currentSong = getCurrentSpotifySong()
        val message = if(currentSong == null || isSpotifyPlaying() == false) {
            "No Song playing right now!"
        } else {
            createSongString(currentSong.name, currentSong.artists) + " --> ${currentSong.externalUrls.spotify}"
        }

        sendMessageToTwitchChatAndLogIt(chat, message)

        addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDown
    }
)