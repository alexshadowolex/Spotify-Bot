package ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import config.BotConfig
import config.TwitchBotConfig
import kotlin.time.Duration.Companion.seconds


private var commandPrefix = mutableStateOf("")
    set(value) {
        field = value
        TwitchBotConfig.commandPrefix = value.value
    }

// TODO: maybe mutablestatelistof?
private var songRequestEmotes = mutableStateOf(listOf<String>())
    set(value) {
        field = value
        TwitchBotConfig.songRequestEmotes = value.value
    }

private var defaultUserCoolDown = mutableStateOf(0.seconds)
    set(value) {
        field = value
        TwitchBotConfig.defaultUserCoolDown = value.value
    }

private var defaultCommandCoolDown = mutableStateOf(0.seconds)
    set(value) {
        field = value
        TwitchBotConfig.defaultCommandCoolDown = value.value
    }

private var blacklistMessage = mutableStateOf("")
    set(value) {
        field = value
        TwitchBotConfig.blacklistMessage = value.value
    }


@Composable
fun twitchSettingsScreen() {

}

@Composable
fun initializeTwitchFlagVariables() {
    commandPrefix = remember { mutableStateOf(TwitchBotConfig.commandPrefix) }
    songRequestEmotes = remember { mutableStateOf(TwitchBotConfig.songRequestEmotes) }
    defaultUserCoolDown = remember { mutableStateOf(TwitchBotConfig.defaultUserCoolDown) }
    defaultCommandCoolDown = remember { mutableStateOf(TwitchBotConfig.defaultCommandCoolDown) }
    blacklistMessage = remember { mutableStateOf(TwitchBotConfig.blacklistMessage) }
}