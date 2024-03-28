package commands

import addQuotationMarks
import config.BotConfig
import config.TwitchBotConfig
import getCurrentSpotifySong
import handler.Command
import isUserEligibleForSkipSongCommand
import logger
import sendMessageToTwitchChatAndLogIt
import spotifyClient

val skipSongCommand: Command = Command(
    commandDisplayName = "Skip Song",
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
        val errorTwitchChatMessage = "An error occurred, couldn't skip song."

        if(currentSong == null) {
            logger.error("Error while getting current song")
            sendMessageToTwitchChatAndLogIt(chat, errorTwitchChatMessage)
            return@Command
        }

        try {
            spotifyClient.player.skipForward()
        } catch (e: Exception) {
            logger.error("Error while skipping song ${currentSong.name.addQuotationMarks()}: ", e)
            sendMessageToTwitchChatAndLogIt(chat, errorTwitchChatMessage)
            return@Command
        }

        sendMessageToTwitchChatAndLogIt(chat, "Skipped song ${currentSong.name.addQuotationMarks()}")

        addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDownSeconds
    }
)