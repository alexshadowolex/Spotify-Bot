package commands

import config.BotConfig
import config.TwitchBotConfig
import handleCommandSanityChecksWithSecurityLevel
import handleCommandSanityChecksWithoutSecurityLevel
import handler.Command
import isUserEligibleForPauseResumeCommand
import isUserEligibleForRemoveSongFromQueueCommand

val pauseResumeCommand: Command = Command(
    commandDisplayName = "Pause/Resume Command",
    names = listOf("pauseresume", "pr", "pause", "resume", "play"),
    handler = {

        if(!handleCommandSanityChecksWithSecurityLevel(
                commandName = "pauseResumeCommand",
                isCommandEnabledFlag= BotConfig.isPauseResumeCommandEnabled,
                messageEvent = messageEvent,
                twitchClient = twitchClient,
                securityCheckFunction = ::isUserEligibleForPauseResumeCommand,
                securityLevel = BotConfig.pauseResumeCommandSecurityLevel,
            )) {
            return@Command
        }



        addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDownSeconds
    }
)