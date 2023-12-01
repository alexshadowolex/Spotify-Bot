package ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
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
import java.awt.Desktop
import java.net.URI
import kotlin.text.Typography
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

var isSongRequestRedeemEnabled = !TwitchBotConfig.isSongRequestCommandEnabledByDefault
var isSongRequestCommandEnabled = TwitchBotConfig.isSongRequestCommandEnabledByDefault
var isSpotifySongNameGetterEnabled = TwitchBotConfig.isSpotifySongNameGetterEnabledByDefault

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

    val isSongRequestChecked = remember { mutableStateOf(TwitchBotConfig.isSongRequestCommandEnabledByDefault) }
    val isSpotifySongNameGetterChecked = remember { mutableStateOf(TwitchBotConfig.isSpotifySongNameGetterEnabledByDefault) }

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
                Row {
                    Column {
                        Row(
                            modifier = Modifier
                                .padding(top = 15.dp)
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
                            ) {
                                Text(
                                    text = "Redeem",
                                    modifier = Modifier
                                        .align(Alignment.Start),
                                    color = if(!isSongRequestChecked.value) {
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
                                    checked = isSongRequestChecked.value,
                                    onCheckedChange = {
                                        isSongRequestChecked.value = it
                                        isSongRequestRedeemEnabled = !it
                                        isSongRequestCommandEnabled = it
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .fillMaxWidth()
                                        .scale(1.5F)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterVertically)
                            ) {
                                Text(
                                    text = "Command",
                                    modifier = Modifier
                                        .align(Alignment.End),
                                    color = if(isSongRequestChecked.value) {
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
                                    if (isSpotifySongNameGetterChecked.value) {
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
                            checked = isSpotifySongNameGetterChecked.value,
                            onCheckedChange = {
                                isSpotifySongNameGetterChecked.value = it
                                isSpotifySongNameGetterEnabled = it
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