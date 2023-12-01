package ui

import GITHUB_LATEST_VERSION_LINK
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import backgroundCoroutineScope
import config.BuildInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import kotlin.time.Duration.Companion.seconds

@Composable
fun newVersionScreen(isNewVersionWindowOpen: MutableState<Boolean>) {
    val scaffoldState = rememberScaffoldState()
    MaterialTheme(colors = darkColorPalette) {
        Scaffold(
            scaffoldState = scaffoldState
        ) {
            Column (
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, start = 20.dp, end = 20.dp, bottom = 10.dp)
                ) {
                    Text(
                        text = "New Version ${BuildInfo.latestAvailableVersion} of this app is available on ",
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                    )

                    Text(
                        style = MaterialTheme.typography.body1,
                        text = "GitHub",
                        modifier = Modifier
                            .clickable {
                                backgroundCoroutineScope.launch {
                                    Toolkit.getDefaultToolkit().systemClipboard.setContents(
                                        StringSelection(GITHUB_LATEST_VERSION_LINK),
                                        null
                                    )

                                    scaffoldState.snackbarHostState.showSnackbar(
                                        message = "URL copied! Opening browser...",
                                        duration = SnackbarDuration.Short
                                    )

                                    delay(1.seconds)

                                    withContext(Dispatchers.IO) {
                                        Desktop.getDesktop().browse(URI.create(GITHUB_LATEST_VERSION_LINK))
                                    }
                                }
                            }
                            .padding(3.dp)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .align(Alignment.CenterVertically),
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colors.secondary
                    )
                }

                Row {
                    Column (
                        modifier = Modifier
                            .fillMaxWidth(0.5F)
                    ) {
                        Button(
                            onClick = {
                                isNewVersionWindowOpen.value = false
                            },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Text("Ignore")
                        }
                    }

                    Column (
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                Desktop.getDesktop().browse(URI.create(GITHUB_LATEST_VERSION_LINK))
                            },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Text("Go To GitHub")
                        }
                    }
                }
            }
        }
    }
}


