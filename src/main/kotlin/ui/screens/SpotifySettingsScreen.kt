package ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import config.SpotifyConfig
import toDoublePropertiesString
import ui.dropDownStringPropertiesList
import ui.propertiesTextField
import ui.sectionDivider
import ui.versionAndCreditsRow
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

private var maximumLengthSongRequestMinutes = mutableStateOf("0")
    set(value) {
        field = value
        SpotifyConfig.maximumLengthSongRequestMinutes = value.value.toDouble().minutes
    }

private var blockedSongArtists = mutableStateListOf("")
    set(value) {
        field = value
        SpotifyConfig.blockedSongArtists = value
    }

private var blockedSongLinks = mutableStateListOf("")
    set(value) {
        field = value
        SpotifyConfig.blockedSongLinks = value
    }

private var playlistIdForAddSongCommand = mutableStateOf("")
    set(value) {
        field = value
        SpotifyConfig.playlistIdForAddSongCommand = value.value
    }


@Composable
fun spotifySettingsScreen() {
    val scaffoldState = rememberScaffoldState()

    MaterialTheme {
        Scaffold(
            scaffoldState = scaffoldState
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 5.dp, bottom = 5.dp, start = 10.dp, end = 10.dp)
            ) {

                propertiesTextField(
                    textFieldTitle = "Maximum Length Song Requests Minutes",
                    textFieldContent = maximumLengthSongRequestMinutes,
                    onValueChange = {
                        maximumLengthSongRequestMinutes.value = try {
                            it.toDouble()
                            it
                        } catch (e: NumberFormatException) {
                            "0"
                        }
                    }
                )

                sectionDivider()

                dropDownStringPropertiesList(
                    entries = blockedSongArtists,
                    textFieldTitle = "Blocked Song Artists",
                    scaffoldState = scaffoldState
                )

                sectionDivider()

                dropDownStringPropertiesList(
                    entries = blockedSongLinks,
                    textFieldTitle = "Blocked Song Links",
                    scaffoldState = scaffoldState
                )

                sectionDivider()

                propertiesTextField(
                    textFieldTitle = "Playlist ID for Add Song Command",
                    textFieldContent = playlistIdForAddSongCommand,
                    onValueChange = {
                        playlistIdForAddSongCommand.value = it
                    }
                )

                sectionDivider()

                versionAndCreditsRow()
            }
        }
    }
}

@Composable
fun initializeSpotifyFlagVariables() {
    maximumLengthSongRequestMinutes = remember { mutableStateOf(
        SpotifyConfig.maximumLengthSongRequestMinutes.toDoublePropertiesString(DurationUnit.MINUTES)
    ) }
    blockedSongArtists = remember { SpotifyConfig.blockedSongArtists.toMutableStateList() }
    blockedSongLinks = remember { SpotifyConfig.blockedSongLinks.toMutableStateList() }
    playlistIdForAddSongCommand = remember { mutableStateOf(SpotifyConfig.playlistIdForAddSongCommand) }
}