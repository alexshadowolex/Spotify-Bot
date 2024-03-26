package commands

import addQuotationMarks
import config.BotConfig
import config.TwitchBotConfig
import getCurrentSpotifySong
import handler.Command
import isUserEligibleForBlockSongCommand
import logger
import sendMessageToTwitchChatAndLogIt
import spotifyClient
import ui.screens.blockedSongLinks

val blockSongCommand: Command = Command(
    names = listOf("blocksong", "block", "bs"),
    handler = {
        if(!BotConfig.isBlockSongCommandEnabled) {
            logger.info("blockSongCommand disabled. Aborting execution")
            return@Command
        }


        if(!isUserEligibleForBlockSongCommand(messageEvent.permissions, messageEvent.user.name)) {
            logger.info("User ${messageEvent.user.name} tried using blockSongCommand but was not eligible. " +
                    "Current security setting: ${BotConfig.blockSongCommandSecurityLevel}"
            )

            sendMessageToTwitchChatAndLogIt(chat, "You are not eligible to use that command!")
            return@Command
        }

        val currentSong = getCurrentSpotifySong()
        val errorTwitchChatMessage = "An error occurred, couldn't block and skip song."

        if(currentSong == null) {
            logger.error("Error while getting current song")
            sendMessageToTwitchChatAndLogIt(chat, errorTwitchChatMessage)
            return@Command
        }

        if(currentSong.externalUrls.spotify == null) {
            logger.error("Error while getting putting the song on block list because it has no share link")
            sendMessageToTwitchChatAndLogIt(chat, errorTwitchChatMessage)
            return@Command
        }

        blockedSongLinks.add(currentSong.externalUrls.spotify!!)

        try {
            spotifyClient.player.skipForward()
        } catch (e: Exception) {
            logger.error("Error while skipping song ${currentSong.name.addQuotationMarks()}: ", e)
            sendMessageToTwitchChatAndLogIt(chat, errorTwitchChatMessage)
            return@Command
        }

        sendMessageToTwitchChatAndLogIt(chat, "Blocked and skipped song ${currentSong.name.addQuotationMarks()}")

        addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDownSeconds
    }
)