package ui

import CustomCommandPermissions
import NavController
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import config.BotConfig
import isWindowsInDarkMode
import kotlinx.coroutines.delay
import rememberNavController
import ui.navigation.NavigationHost
import ui.navigation.composable
import ui.screens.generalSettingsScreen
import ui.screens.homeScreen
import kotlin.time.Duration.Companion.seconds

val darkColorPalette = darkColors(
    primary = Color(0xff5bbbfe),
    onPrimary = Color.White,
    secondary = Color(0xff2244bb),
    background = Color.DarkGray,
    onBackground = Color.White,
)

val lightColorPalette = lightColors(
    primary = Color(0xff4466ff),
    onPrimary = Color.White,
    secondary = Color(0xff0b5b8e),
    background = Color.White,
    onBackground = Color.Black,
)

var isSongRequestEnabled = mutableStateOf(false)
    set(value) {
        field = value
        BotConfig.isSongRequestEnabled = value.value
    }
var isSongRequestEnabledAsCommand = mutableStateOf(false)
    set(value) {
        field = value
        BotConfig.isSongRequestCommandEnabled = value.value
    }
var isSpotifySongNameGetterEnabled = mutableStateOf(false)
    set(value) {
        field = value
        BotConfig.isSpotifySongNameGetterEnabled = value.value
    }
var isSongInfoCommandEnabled = mutableStateOf(false)
    set(value) {
        field = value
        BotConfig.isSongInfoCommandEnabled = value.value
    }
var isEmptySongDisplayFilesOnPauseEnabled = mutableStateOf(false)
    set(value) {
        field = value
        BotConfig.isEmptySongDisplayFilesOnPauseEnabled = value.value
    }
var isAddSongCommandEnabled = mutableStateOf(false)
    set(value) {
        field = value
        BotConfig.isAddSongCommandEnabled = value.value
    }
var addSongCommandSecurityLevel = mutableStateOf(CustomCommandPermissions.BROADCASTER)
    set(value) {
        field = value
        BotConfig.addSongCommandSecurityLevel = value.value
    }
var isSkipSongCommandEnabled = mutableStateOf(false)
    set(value) {
        field = value
        BotConfig.isSkipSongCommandEnabled = value.value
    }
var skipSongCommandSecurityLevel = mutableStateOf(CustomCommandPermissions.BROADCASTER)
    set(value) {
        field = value
        BotConfig.skipSongCommandSecurityLevel = value.value
    }
var isRemoveSongFromQueueCommandEnabled = mutableStateOf(false)
    set(value) {
        field = value
        BotConfig.isRemoveSongFromQueueCommandEnabled = value.value
    }
var removeSongFromQueueCommandSecurityLevel = mutableStateOf(CustomCommandPermissions.BROADCASTER)
    set(value) {
        field = value
        BotConfig.removeSongFromQueueCommandSecurityLevel = value.value
    }


@Composable
@Preview
fun app() {
    var isInDarkMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isInDarkMode = isWindowsInDarkMode()
            delay(1.seconds)
        }
    }

    initializeFlagVariables()

    val screens = Screen.values().toList()
    val navController by rememberNavController(Screen.HomeScreen.name)
    val currentScreen by remember { navController.currentScreen }

    MaterialTheme(
        colors = if (isInDarkMode) {
            darkColorPalette
        } else {
            lightColorPalette
        }
    ) {
        Scaffold {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Row {
                        BottomNavigation(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            screens.forEach {
                                BottomNavigationItem(
                                    selected = currentScreen == it.name,
                                    icon = {
                                        Icon(
                                            imageVector = it.icon,
                                            contentDescription = it.label
                                        )
                                    },
                                    label = {
                                        Text(it.label)
                                    },
                                    alwaysShowLabel = true,
                                    onClick = {
                                        navController.navigate(it.name)
                                    }
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    customNavigationHost(
                        navController = navController
                    )
                }
            }
        }
    }
}


// Navigation
enum class Screen(
    val label: String,
    val icon: ImageVector
) {
    HomeScreen(
        label = "Home",
        icon = Icons.Filled.Home
    ),
    GeneralSettingsScreen(
        label = "General Settings",
        icon = Icons.Filled.Settings
    )
}

@Composable
fun customNavigationHost(
    navController: NavController
) {
    NavigationHost(navController) {
        composable(Screen.HomeScreen.name) {
            homeScreen()
        }
    }.build()

    NavigationHost(navController) {
        composable(Screen.GeneralSettingsScreen.name) {
            generalSettingsScreen()
        }
    }.build()
}

@Composable
fun initializeFlagVariables() {
    isSongRequestEnabled = remember { mutableStateOf(BotConfig.isSongRequestEnabled) }
    isSongRequestEnabledAsCommand = remember { mutableStateOf(BotConfig.isSongRequestCommandEnabled) }
    isSpotifySongNameGetterEnabled = remember { mutableStateOf(BotConfig.isSpotifySongNameGetterEnabled) }
    isSongInfoCommandEnabled = remember { mutableStateOf(BotConfig.isSongInfoCommandEnabled) }
    isEmptySongDisplayFilesOnPauseEnabled = remember { mutableStateOf(BotConfig.isEmptySongDisplayFilesOnPauseEnabled) }
    isAddSongCommandEnabled = remember { mutableStateOf(BotConfig.isAddSongCommandEnabled) }
    addSongCommandSecurityLevel = remember { mutableStateOf(BotConfig.addSongCommandSecurityLevel) }
    isSkipSongCommandEnabled = remember { mutableStateOf(BotConfig.isSkipSongCommandEnabled) }
    skipSongCommandSecurityLevel = remember { mutableStateOf(BotConfig.skipSongCommandSecurityLevel) }
    isRemoveSongFromQueueCommandEnabled = remember { mutableStateOf(BotConfig.isRemoveSongFromQueueCommandEnabled) }
    removeSongFromQueueCommandSecurityLevel = remember { mutableStateOf(BotConfig.removeSongFromQueueCommandSecurityLevel) }
}