package commands

import config.BotConfig
import config.TwitchBotConfig
import getCurrentSpotifySong
import handler.Command
import isUserEligibleForSkipSongCommand
import logger
import sendMessageToTwitchChatAndLogIt
import spotifyClient

val skipSongCommand: Command = Command(
    names = listOf("skipsong", "skip", "next", "ss"),
    handler = {
        if(!BotConfig.isSkipSongCommandEnabled) {
            logger.info("skipSongCommand disabled. Aborting execution")
            return@Command
        }


        if(!isUserEligibleForSkipSongCommand(messageEvent.permissions, messageEvent.user.name)) {
            logger.info("User ${messageEvent.user.name} tried using skipSongCommand but was not eligible. " +
                    "Current security setting: ${BotConfig.skipSongCommandSecurityLevel}"
            )

            sendMessageToTwitchChatAndLogIt(chat, "You are not eligible to use that command!")
            return@Command
        }

        val currentSong = getCurrentSpotifySong()
        try {
            spotifyClient.player.skipForward()
        } catch (e: Exception) {
            logger.error("Error while skipping song ${currentSong?.name ?: ""}: ", e)
            sendMessageToTwitchChatAndLogIt(chat, "An error occurred, couldn't skip song.")
            return@Command
        }

        sendMessageToTwitchChatAndLogIt(chat, "Skipped song ${currentSong?.name ?: ""}")

        addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDownSeconds
    }
)