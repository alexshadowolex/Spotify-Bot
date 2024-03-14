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

private var defaultUserCoolDownSeconds = mutableStateOf("0")
    set(value) {
        field = value
        TwitchBotConfig.defaultUserCoolDownSeconds = value.value.toInt().seconds
    }

private var defaultCommandCoolDownSeconds = mutableStateOf("0")
    set(value) {
        field = value
        TwitchBotConfig.defaultCommandCoolDownSeconds = value.value.toInt().seconds
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
    defaultUserCoolDownSeconds = remember { mutableStateOf(
        TwitchBotConfig.defaultUserCoolDownSeconds.toIntPropertiesString(DurationUnit.SECONDS)
    ) }
    defaultCommandCoolDownSeconds = remember { mutableStateOf(
        TwitchBotConfig.defaultCommandCoolDownSeconds.toIntPropertiesString(DurationUnit.SECONDS)
    ) }
    blacklistMessage = remember { mutableStateOf(TwitchBotConfig.blacklistMessage) }
}