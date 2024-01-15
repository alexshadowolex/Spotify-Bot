package commands

import addSongToPlaylist
import config.TwitchBotConfig
import getCurrentSpotifySong
import handler.Command

val addSongCommand: Command = Command(
    names = listOf("addsong", "as", "add"),
    handler = {
        val currentSong = getCurrentSpotifySong()
        val success = if (currentSong != null) {
            addSongToPlaylist(currentSong)
        } else {
            false
        }

        val message = if(success) {
            "Successfully added song ${currentSong!!.name} to the playlist!"
        } else {
            "Something went wrong when adding the song to the playlist."
        }

        chat.sendMessage(TwitchBotConfig.channel, message)

        addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDown
    }
)