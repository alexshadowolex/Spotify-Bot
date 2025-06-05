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
import backgroundCoroutineScope
import config.BuildInfo
import isWindowsInDarkMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logger
import prepareAndStartAutoUpdate
import ui.alertDialogSurface
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

    var isAlertDialogVisible = mutableStateOf(false)
    var alertDialogTitle = mutableStateOf("")
    var alertDialogMessage = mutableStateOf("")
    var alertDialogOnOkClick = mutableStateOf({})

    MaterialTheme(
        colors = if (isInDarkMode) {
            darkColorPalette
        } else {
            lightColorPalette
        }
    ) {
        val scaffoldState = rememberScaffoldState()
        Scaffold (
            scaffoldState = scaffoldState
        ) {
            alertDialogSurface(
                isVisible = isAlertDialogVisible,
                title = alertDialogTitle,
                message = alertDialogMessage,
                onOkClick = alertDialogOnOkClick
            )

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
                            Column {
                                Button(
                                    onClick = {
                                        logger.info("Clicked on don't show again button")
                                        alertDialogTitle.value = "Note"
                                        alertDialogMessage.value = "This will disable the check for new Versions on start up." +
                                                " You can always enable this again in the general settings screen."
                                        alertDialogOnOkClick.value = {
                                            isNewVersionWindowOpen.value = false
                                            isNewVersionCheckEnabled.value = false
                                            isAlertDialogVisible.value = false
                                        }
                                        isAlertDialogVisible.value = true
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

                            Column {
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

                            Column {
                                val isUpdateButtonEnabled = remember {mutableStateOf(true) }
                                Button(
                                    onClick = {
                                        logger.info("Clicked on Update Button")
                                        alertDialogTitle.value = "Note"
                                        alertDialogMessage.value = "This will automatically update this Bot to the newest " +
                                                "version. It will download the newest version of update-script from GitHub, if it is " +
                                                "not existing already, and then execute it.\n" +
                                                "The update-script will close the Spotify-Bot, open several consoles (do not be scared, " +
                                                "that is supposed to happen), update everything and launch the new Spotify-Bot-version.\n" +
                                                "In the process, old versions of the Spotify-Bot- and UpdateProperties-Jars will " +
                                                "be deleted from the base directory. If you want to keep them, move them somewhere " +
                                                "else. Keep in mind that they are always available online."
                                        alertDialogOnOkClick.value = {
                                            isAlertDialogVisible.value = false
                                            val isAutoUpdateSuccessful = prepareAndStartAutoUpdate()

                                            if(!isAutoUpdateSuccessful) {
                                                isUpdateButtonEnabled.value = false

                                                backgroundCoroutineScope.launch {
                                                    scaffoldState.snackbarHostState.showSnackbar(
                                                        message = "Error while starting auto-update. Disabling auto-update button. Check the logs!",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            }
                                        }
                                        isAlertDialogVisible.value = true
                                    },
                                    enabled = isUpdateButtonEnabled.value,
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


