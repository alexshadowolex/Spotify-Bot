package ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import config.TwitchBotConfig
import toDoublePropertiesString
import toIntPropertiesString
import ui.dropDownStringPropertiesList
import ui.propertiesTextField
import ui.sectionDivider
import ui.versionAndCreditsRow
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit


private var commandPrefix = mutableStateOf("")
    set(value) {
        field = value
        TwitchBotConfig.commandPrefix = value.value
    }

private var songRequestEmotes = mutableStateListOf("")
    set(value) {
        field = value
        TwitchBotConfig.songRequestEmotes = value
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

private var songRequestRedeemId = mutableStateOf("")
    set(value) {
        field = value
        TwitchBotConfig.songRequestRedeemId = value.value
    }

private var minimumFollowingDurationMinutes = mutableStateOf("0")
    set(value) {
        field = value
        TwitchBotConfig.minimumFollowingDurationMinutes = value.value.toDouble().minutes
    }


@Composable
fun twitchSettingsScreen() {
    val scaffoldState = rememberScaffoldState()

    MaterialTheme {
        Scaffold (
            scaffoldState = scaffoldState
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 5.dp, bottom = 5.dp, start = 10.dp, end = 10.dp)
            ) {
                propertiesTextField(
                    textFieldTitle = "Command Prefix",
                    textFieldContent = commandPrefix,
                    onValueChange = {
                        commandPrefix.value = it
                    }
                )

                sectionDivider()

                propertiesTextField(
                    textFieldTitle = "Default Command Cool Down Seconds",
                    textFieldContent = defaultCommandCoolDownSeconds,
                    onValueChange = {
                        defaultCommandCoolDownSeconds.value = try {
                            it.toInt()
                            it.replace("-", "")
                        } catch (_: NumberFormatException) {
                            "0"
                        }
                    }
                )

                sectionDivider()

                propertiesTextField(
                    textFieldTitle = "Default User Cool Down Seconds",
                    textFieldContent = defaultUserCoolDownSeconds,
                    onValueChange = {
                        defaultUserCoolDownSeconds.value = try {
                            it.toInt()
                            it.replace("-", "")
                        } catch (_: NumberFormatException) {
                            "0"
                        }
                    }
                )

                sectionDivider()

                dropDownStringPropertiesList(
                    entries = songRequestEmotes,
                    textFieldTitle = "Song Request Emotes",
                    scaffoldState = scaffoldState,
                    lowercaseInput = false
                )

                sectionDivider()

                propertiesTextField(
                    textFieldTitle = "Blacklist Message",
                    textFieldContent = blacklistMessage,
                    onValueChange = {
                        blacklistMessage.value = it
                    }
                )

                sectionDivider()

                propertiesTextField(
                    textFieldTitle = "Song Request Redeem ID",
                    textFieldContent = songRequestRedeemId,
                    onValueChange = {
                        songRequestRedeemId.value = it
                    }
                )

                sectionDivider()

                propertiesTextField(
                    textFieldTitle = "Minimum Following Duration in Minutes For Follower Only Mode",
                    textFieldContent = minimumFollowingDurationMinutes,
                    onValueChange = {
                        minimumFollowingDurationMinutes.value = try {
                            it.toDouble()
                            it.replace("-", "")
                        } catch (_: NumberFormatException) {
                            "0"
                        }
                    }
                )

                sectionDivider()

                versionAndCreditsRow()
            }
        }
    }
}


@Composable
fun initializeTwitchFlagVariables() {
    commandPrefix = remember { mutableStateOf(TwitchBotConfig.commandPrefix) }
    songRequestEmotes = remember { TwitchBotConfig.songRequestEmotes.toMutableStateList() }
    defaultUserCoolDownSeconds = remember { mutableStateOf(
        TwitchBotConfig.defaultUserCoolDownSeconds.toIntPropertiesString(DurationUnit.SECONDS)
    ) }
    defaultCommandCoolDownSeconds = remember { mutableStateOf(
        TwitchBotConfig.defaultCommandCoolDownSeconds.toIntPropertiesString(DurationUnit.SECONDS)
    ) }
    blacklistMessage = remember { mutableStateOf(TwitchBotConfig.blacklistMessage) }
    songRequestRedeemId = remember { mutableStateOf(TwitchBotConfig.songRequestRedeemId) }
    minimumFollowingDurationMinutes = remember { mutableStateOf(
        TwitchBotConfig.minimumFollowingDurationMinutes.toDoublePropertiesString(DurationUnit.MINUTES)
    ) }
}