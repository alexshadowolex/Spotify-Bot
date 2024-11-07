package commands

import addQuotationMarks
import config.BotConfig
import config.TwitchBotConfig
import getCurrentSpotifySong
import handleCommandSanityChecksWithSecurityLevel
import handler.Command
import isUserEligibleForSkipSongCommand
import logger
import sendMessageToTwitchChatAndLogIt
import spotifyClient

val skipSongCommand: Command = Command(
    commandDisplayName = "Skip Song",
    names = listOf("skipsong", "skip", "next", "ss"),
    handler = {

        if(!handleCommandSanityChecksWithSecurityLevel(
            commandName = "skipSongCommand",
            isCommandEnabledFlag = BotConfig.isSkipSongCommandEnabled,
            messageEvent = messageEvent,
            twitchClient = twitchClient,
            securityCheckFunction = ::isUserEligibleForSkipSongCommand,
            securityLevel = BotConfig.skipSongCommandSecurityLevel
        )) {
            return@Command
        }

        val currentSong = getCurrentSpotifySong()
        val errorTwitchChatMessage = "An error occurred, couldn't skip song."

        if(currentSong == null) {
            logger.error("Error while getting current song")
            sendMessageToTwitchChatAndLogIt(twitchClient.chat, errorTwitchChatMessage)
            return@Command
        }

        try {
            spotifyClient.player.skipForward()
        } catch (e: Exception) {
            logger.error("Error while skipping song ${currentSong.name.addQuotationMarks()}: ", e)
            sendMessageToTwitchChatAndLogIt(twitchClient.chat, errorTwitchChatMessage)
            return@Command
        }

        sendMessageToTwitchChatAndLogIt(twitchClient.chat, "Skipped song ${currentSong.name.addQuotationMarks()}")

        addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDownSeconds
    }
)