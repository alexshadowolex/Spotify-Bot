package ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import config.BotConfig
import config.SpotifyConfig
import kotlin.time.Duration.Companion.minutes

private var maximumLengthSongRequestMinutes = mutableStateOf(0.minutes)
    set(value) {
        field = value
        SpotifyConfig.maximumLengthSongRequestMinutes = value.value
    }
// TODO: maybe mutablestatelistof?
private var blockedSongArtists = mutableStateOf(listOf<String>())
    set(value) {
        field = value
        SpotifyConfig.blockedSongArtists = value.value
    }
// TODO: maybe mutablestatelistof?
private var blockedSongLinks = mutableStateOf(listOf<String>())
    set(value) {
        field = value
        SpotifyConfig.blockedSongLinks = value.value
    }

private var playlistIdForAddSongCommand = mutableStateOf("")
    set(value) {
        field = value
        SpotifyConfig.playlistIdForAddSongCommand = value.value
    }


@Composable
fun spotifySettingsScreen() {

}

@Composable
fun initializeSpotifyFlagVariables() {
    maximumLengthSongRequestMinutes = remember { mutableStateOf(SpotifyConfig.maximumLengthSongRequestMinutes) }
    blockedSongArtists = remember { mutableStateOf(SpotifyConfig.blockedSongArtists) }
    blockedSongLinks = remember { mutableStateOf(SpotifyConfig.blockedSongLinks) }
    playlistIdForAddSongCommand = remember { mutableStateOf(SpotifyConfig.playlistIdForAddSongCommand) }
}