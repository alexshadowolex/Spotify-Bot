package commands

import addQuotationMarks
import config.BotConfig
import config.TwitchBotConfig
import getCurrentSpotifySong
import handleCommandSanityChecksWithSecurityLevel
import handler.Command
import isUserEligibleForBlockSongCommand
import logger
import sendMessageToTwitchChatAndLogIt
import spotifyClient
import ui.screens.blockedSongLinks

val blockSongCommand: Command = Command(
    commandDisplayName = "Block Song",
    names = listOf("blocksong", "block", "bs"),
    handler = {

        if(!handleCommandSanityChecksWithSecurityLevel(
                commandName = "blockSongCommand",
                isCommandEnabledFlag= BotConfig.isBlockSongCommandEnabled,
                messageEvent = messageEvent,
                twitchClient = twitchClient,
                securityCheckFunction = ::isUserEligibleForBlockSongCommand,
                securityLevel = BotConfig.blockSongCommandSecurityLevel
            )) {
            return@Command
        }

        val currentSong = getCurrentSpotifySong()
        val errorTwitchChatMessage = "An error occurred, couldn't block and skip song."

        if(currentSong == null) {
            logger.error("Error while getting current song")
            sendMessageToTwitchChatAndLogIt(twitchClient.chat, errorTwitchChatMessage)
            return@Command
        }

        if(currentSong.externalUrls.spotify == null) {
            logger.error("Error while getting putting the song on block list because it has no share link")
            sendMessageToTwitchChatAndLogIt(twitchClient.chat, errorTwitchChatMessage)
            return@Command
        }

        blockedSongLinks.add(currentSong.externalUrls.spotify!!)

        try {
            spotifyClient.player.skipForward()
        } catch (e: Exception) {
            logger.error("Error while skipping song ${currentSong.name.addQuotationMarks()}: ", e)
            sendMessageToTwitchChatAndLogIt(twitchClient.chat, errorTwitchChatMessage)
            return@Command
        }

        sendMessageToTwitchChatAndLogIt(twitchClient.chat, "Blocked and skipped song ${currentSong.name.addQuotationMarks()}")

        addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDownSeconds
    }
)