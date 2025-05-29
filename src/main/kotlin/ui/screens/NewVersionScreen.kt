package ui.screens

import GITHUB_LATEST_VERSION_LINK
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import config.BuildInfo
import isWindowsInDarkMode
import kotlinx.coroutines.delay
import logger
import ui.darkColorPalette
import ui.lightColorPalette
import java.awt.Desktop
import java.net.URI
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalFoundationApi::class)
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
                        text = "You currently have Version v${BuildInfo.version}. New Version " +
                                "v${BuildInfo.latestAvailableVersion} of this app is available on GitHub!",
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                    )
                }


                if(BuildInfo.releaseBodyText != null) {
                    Row (
                        modifier = Modifier
                            .padding(top = 10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Changes in this version: "
                            )
                            val scrollState = rememberScrollState(0)
                            val scrollBarAdapter = rememberScrollbarAdapter(scrollState)
                            Column(
                                modifier = Modifier
                                    .height(300.dp)
                                    .fillMaxWidth()
                                    .padding(top = 5.dp)
                                    .border(1.dp, Color(30, 30, 30), RoundedCornerShape(4.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(all = 5.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth(0.95f)
                                            .verticalScroll(scrollState)
                                    ) {
                                        Text(
                                            text = BuildInfo.releaseBodyText!!,
                                            modifier = Modifier
                                                .padding(all = 5.dp)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    ) {
                                        VerticalScrollbar(
                                            adapter = scrollBarAdapter,
                                            modifier = Modifier
                                                .align(Alignment.End)
                                                .padding(start = 2.dp)
                                                .padding(end = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
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
                        Row (
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                            ) {
                                Button(
                                    onClick = {
                                        logger.info("Clicked on don't show again button")
                                        isNewVersionWindowOpen.value = false
                                        isNewVersionCheckEnabled.value = false
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .pointerHoverIcon(PointerIcon.Hand),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
                                ) {
                                    Text(
                                        text = "Don't show again",
                                        color = MaterialTheme.colors.onPrimary
                                    )
                                }

                            }

                            Column(
                            ) {
                                Button(
                                    onClick = {
                                        logger.info("Clicked on Go To GitHub Button")
                                        try {
                                            Desktop.getDesktop().browse(URI.create(GITHUB_LATEST_VERSION_LINK))
                                        } catch (e: java.io.IOException) {
                                            logger.error("Couldn't open GitHub-link with button in browser.")
                                            logger.error(e.stackTraceToString())
                                        }
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

                            Column(
                            ) {
                                Button(
                                    onClick = {
                                        logger.info("Clicked on Update Button")
                                        // TODO
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .pointerHoverIcon(PointerIcon.Hand),
                                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
                                ) {
                                    Text(
                                        text = "Update",
                                        color = MaterialTheme.colors.onPrimary,
                                        textDecoration = TextDecoration.Underline
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


