package ui

import ui.navigation.NavController
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import isWindowsInDarkMode
import kotlinx.coroutines.delay
import mainWindowState
import ui.navigation.rememberNavController
import ui.navigation.NavigationHost
import ui.navigation.composable
import ui.screens.*
import kotlin.time.Duration.Companion.seconds


val darkColorPalette = darkColors(
    primary = Color(0xff5bbbfe),
    onSecondary = Color(84, 84, 84),
    onPrimary = Color.White,
    secondary = Color(0xff2244bb),
    background = Color.DarkGray,
    onBackground = Color.White,
)

val lightColorPalette = lightColors(
    primary = Color(0xff4466ff),
    onSecondary = Color(220, 220, 220),
    onPrimary = Color.Black,
    secondary = Color(0xff0b5b8e),
    background = Color.White,
    onBackground = Color.Black,
)


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

    val screens = Screen.entries
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
                        BottomNavigation {
                            Row (
                                modifier = Modifier
                                    .width(500.dp)
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

                                            mainWindowState.value = WindowState(
                                                size = DpSize(it.width, it.height),
                                                position = mainWindowState.value.position
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    customNavigationHost(navController = navController)
                }
            }
        }
    }
}


// Navigation
enum class Screen(
    val label: String,
    val icon: ImageVector,
    val height: Dp,
    val width: Dp
) {
    HomeScreen(
        label = "Home",
        icon = Icons.Filled.Home,
        height = 650.dp,
        width = 500.dp
    ),
    GeneralSettingsScreen(
        label = "General Settings",
        icon = Icons.Filled.Settings,
        height = 850.dp,
        width = 900.dp
    ),
    SpotifySettingsScreen(
        label = "Spotify Settings",
        icon = Icons.Filled.PlayArrow,
        height = 450.dp,
        width = 500.dp
    ),
    TwitchSettingsScreen(
        label = "Twitch Settings",
        icon = Icons.Filled.Edit,
        height = 640.dp,
        width = 500.dp
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

    NavigationHost(navController) {
        composable(Screen.SpotifySettingsScreen.name) {
            spotifySettingsScreen()
        }
    }.build()

    NavigationHost(navController) {
        composable(Screen.TwitchSettingsScreen.name) {
            twitchSettingsScreen()
        }
    }.build()
}

@Composable
fun initializeFlagVariables() {
    initializeGeneralFlagVariables()
    initializeSpotifyFlagVariables()
    initializeTwitchFlagVariables()
}