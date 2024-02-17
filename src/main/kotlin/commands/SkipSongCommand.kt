package commands

import areUsersPermissionsEligibleForSkipSongCommand
import config.TwitchBotConfig
import getCurrentSpotifySong
import handler.Command
import logger
import sendMessageToTwitchChatAndLogIt
import spotifyClient
import ui.isSkipSongCommandEnabled
import ui.skipSongCommandSecurityLevel

val skipSongCommand: Command = Command(
    names = listOf("skipSong", "skip", "next", "ss"),
    handler = {
        if(!isSkipSongCommandEnabled.value) {
            logger.info("skipSongCommand disabled. Aborting execution")
            return@Command
        }


        if(!areUsersPermissionsEligibleForSkipSongCommand(messageEvent.permissions)) {
            logger.info("User ${messageEvent.user.name} tried using skipSongCommand but was not eligible. " +
                    "Current security setting: ${skipSongCommandSecurityLevel.value}"
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

        addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDown
    }
)