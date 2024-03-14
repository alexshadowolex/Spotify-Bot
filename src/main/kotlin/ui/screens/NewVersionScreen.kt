package ui.screens

import GITHUB_LATEST_VERSION_LINK
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import backgroundCoroutineScope
import config.BuildInfo
import isWindowsInDarkMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logger
import ui.darkColorPalette
import ui.lightColorPalette
import java.awt.Desktop
import java.net.URI
import kotlin.time.Duration.Companion.seconds

@Composable
fun newVersionScreen(isNewVersionWindowOpen: MutableState<Boolean>) {
    var isInDarkMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isInDarkMode = isWindowsInDarkMode()
            delay(1.seconds)
        }
    }

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
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 10.dp)
            ) {
                Row {
                    Text(
                        text = "You currently have Version v${BuildInfo.version}.",
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                    )
                }

                Row {
                    Text(
                        text = "New Version v${BuildInfo.latestAvailableVersion} of this app is available on ",
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                    )

                    Text(
                        style = MaterialTheme.typography.body1,
                        text = "GitHub",
                        modifier = Modifier
                            .clickable {
                                logger.info("Clicked on Go To GitHub Link")
                                backgroundCoroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        Desktop.getDesktop().browse(URI.create(GITHUB_LATEST_VERSION_LINK))
                                    }
                                }
                            }
                            .padding(3.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .align(Alignment.CenterVertically),
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colors.primary
                    )
                }

                Row (
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Column (
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Bottom)
                    ) {
                        Row {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.5F)
                            ) {
                                Button(
                                    onClick = {
                                        logger.info("Clicked on Ignore Button")
                                        isNewVersionWindowOpen.value = false
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .pointerHoverIcon(PointerIcon.Hand),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
                                ) {
                                    Text(
                                        text = "Ignore",
                                        color = MaterialTheme.colors.onPrimary
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        logger.info("Clicked on Go To GitHub Button")
                                        Desktop.getDesktop().browse(URI.create(GITHUB_LATEST_VERSION_LINK))
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .pointerHoverIcon(PointerIcon.Hand),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
                                ) {
                                    Text(
                                        text = "Go To GitHub",
                                        color = MaterialTheme.colors.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


