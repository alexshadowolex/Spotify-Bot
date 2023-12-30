package commands

import config.TwitchBotConfig
import createSongString
import getCurrentSpotifySong
import handler.Command
import logger
import ui.isSongInfoCommandEnabled

val songInfoCommand: Command = Command(
    names = listOf("songinfo", "si"),
    handler = {
        if(!isSongInfoCommandEnabled.value) {
            logger.info("SongInfoCommand disabled. Aborting execution")
            return@Command
        }

        val currentSong = getCurrentSpotifySong()
        val message = if(currentSong == null) {
            "No Song playing right now!"
        } else {
            createSongString(currentSong.name, currentSong.artists) + " --> ${currentSong.externalUrls.spotify}"
        }

        chat.sendMessage(TwitchBotConfig.channel, message)

        addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDown
    }
)