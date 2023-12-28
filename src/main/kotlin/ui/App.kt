package ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import backgroundCoroutineScope
import com.github.tkuenneth.nativeparameterstoreaccess.NativeParameterStoreAccess
import com.github.tkuenneth.nativeparameterstoreaccess.WindowsRegistry
import config.BuildInfo
import config.TwitchBotConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logger
import java.awt.Desktop
import java.net.URI
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

lateinit var isSongRequestEnabled: MutableState<Boolean>
lateinit var isSongRequestEnabledAsCommand: MutableState<Boolean>
lateinit var isSpotifySongNameGetterEnabled: MutableState<Boolean>

@Composable
@Preview
fun app() {
    var isInDarkMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isInDarkMode = if (NativeParameterStoreAccess.IS_WINDOWS) {
                WindowsRegistry
                    .getWindowsRegistryEntry(
                        "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                        "AppsUseLightTheme"
                    ) == 0x0
            } else {
                false
            }

            delay(1.seconds)
        }
    }

    isSongRequestEnabled = remember { mutableStateOf(TwitchBotConfig.isSongRequestEnabledByDefault) }
    isSongRequestEnabledAsCommand = remember { mutableStateOf(TwitchBotConfig.isSongRequestCommandEnabledByDefault) }
    isSpotifySongNameGetterEnabled = remember { mutableStateOf(TwitchBotConfig.isSpotifySongNameGetterEnabledByDefault) }

    MaterialTheme(
        colors = if (isInDarkMode) {
            darkColorPalette
        } else {
            lightColorPalette
        }
    ) {
        Scaffold {
            Column (
                modifier = Modifier
                    .padding(top = 5.dp, bottom = 5.dp, start = 10.dp, end = 10.dp)
            ) {
                Row (
                    modifier = Modifier
                        .padding(top = 5.dp)
                ) {
                    Column {
                        Row {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.7F)
                                    .align(Alignment.CenterVertically)
                            ) {
                                Text(
                                    text = "Song Request " +
                                            if (isSongRequestEnabled.value) {
                                                "Enabled"
                                            } else {
                                                "Disabled"
                                            },
                                    modifier = Modifier
                                        .align(Alignment.Start)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterVertically)
                            ) {
                                Switch(
                                    checked = isSongRequestEnabled.value,
                                    onCheckedChange = {
                                        logger.info("Clicked on isSongRequestEnabled Switch")
                                        isSongRequestEnabled.value = it
                                    },
                                    modifier = Modifier
                                        .align(Alignment.End)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .padding(top = 3.dp)
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = "Song Request as...",
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                )
                            }
                        }

                        Row (
                            modifier = Modifier
                                .padding(top = 5.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.33F)
                                    .align(Alignment.CenterVertically)
                                    .padding(start = 15.dp)
                            ) {
                                Text(
                                    text = "Redeem",
                                    modifier = Modifier
                                        .align(Alignment.Start),
                                    color = if(!isSongRequestEnabledAsCommand.value) {
                                        MaterialTheme.colors.primary
                                    } else {
                                        MaterialTheme.colors.onBackground
                                    }
                                )
                            }

                            Column (
                                modifier = Modifier
                                    .fillMaxWidth(0.5F)
                                    .align(Alignment.CenterVertically)
                            ) {
                                Switch(
                                    checked = isSongRequestEnabledAsCommand.value,
                                    onCheckedChange = {
                                        logger.info("Clicked on songRequestTypeChecked Switch")
                                        isSongRequestEnabledAsCommand.value = it
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .fillMaxWidth()
                                        .scale(1.5F),
                                    enabled = isSongRequestEnabled.value
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterVertically)
                                    .padding(end = 15.dp)
                            ) {
                                Text(
                                    text = "Command",
                                    modifier = Modifier
                                        .align(Alignment.End),
                                    color = if(isSongRequestEnabledAsCommand.value) {
                                        MaterialTheme.colors.primary
                                    } else {
                                        MaterialTheme.colors.onBackground
                                    }
                                )
                            }
                        }
                    }
                }

                sectionDivider()

                Row(
                    modifier = Modifier
                        .padding(top = 5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.7F)
                            .align(Alignment.CenterVertically)
                    ) {
                        Text(
                            text = "Song Name Getter " +
                                    if (isSpotifySongNameGetterEnabled.value) {
                                        "Enabled"
                                    } else {
                                        "Disabled"
                                    },
                            modifier = Modifier
                                .align(Alignment.Start)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterVertically)
                    ) {
                        Switch(
                            checked = isSpotifySongNameGetterEnabled.value,
                            onCheckedChange = {
                                logger.info("Clicked on isSpotifySongNameGetterEnabled Switch")
                                isSpotifySongNameGetterEnabled.value = it
                            },
                            modifier = Modifier
                                .align(Alignment.End)
                        )
                    }
                }

                sectionDivider()

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Column (
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Bottom)
                    ) {
                        Row (
                            modifier = Modifier
                                .align(Alignment.End),
                        ) {
                            Text(
                                text = "Bot Version v${BuildInfo.version} by ",
                                fontSize = 12.sp
                            )

                            Text(
                                style = MaterialTheme.typography.body1,
                                text = "alexshadowolex",
                                modifier = Modifier
                                    .clickable {
                                        logger.info("Clicked on alexshadowolex Link")
                                        backgroundCoroutineScope.launch {
                                            withContext(Dispatchers.IO) {
                                                Desktop.getDesktop()
                                                    .browse(URI.create("https://www.twitch.tv/alexshadowolex"))
                                            }
                                        }
                                    }
                                    .pointerHoverIcon(PointerIcon.Hand),
                                textDecoration = TextDecoration.Underline,
                                color = MaterialTheme.colors.primary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun sectionDivider() {
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 5.dp),
        color = MaterialTheme.colors.primary,
        thickness = 2.dp
    )
}