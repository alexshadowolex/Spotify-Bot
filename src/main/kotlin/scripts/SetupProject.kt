package scripts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import kotlinx.coroutines.*
import java.awt.Desktop
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URI
import javax.swing.JOptionPane
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

// Current Version: v2

fun main() = try {

    application {
        Window(
            state = WindowState(size = DpSize(600.dp, 400.dp)),
            title = "Setup Project",
            resizable = false,
            onCloseRequest = ::exitApplication,
        ) {
            app3()
        }
    }
} catch (e: Exception) {
    JOptionPane.showMessageDialog(null, e.message + "\n" + StringWriter().also { e.printStackTrace(PrintWriter(it)) }, "InfoBox: File Debugger", JOptionPane.INFORMATION_MESSAGE)
    exitProcess(0)
}


@Composable
fun app3() {
    var twitchToken by remember { mutableStateOf("") }
    var channel by remember { mutableStateOf("") }
    var spotifyId by remember { mutableStateOf("") }
    var spotifySecret by remember { mutableStateOf("") }
    var isSetupInProgress by remember { mutableStateOf(false) }
    val scaffoldState = rememberScaffoldState()
    val backgroundCoroutineScope = CoroutineScope(Dispatchers.IO)

    MaterialTheme {
        Scaffold (
            scaffoldState = scaffoldState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Row (
                    modifier = Modifier
                        .padding(top = 5.dp)
                ) {
                    TextField(
                        label = {
                            Text(
                                text = "Twitch Token"
                            )
                        },
                        value = twitchToken,
                        onValueChange = {
                            twitchToken = it
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp)
                ) {
                    Text(
                        text = "Get twitch token from here: "
                    )

                    Text(
                        style = MaterialTheme.typography.body1,
                        text = "twitchtokengenerator.com",
                        modifier = Modifier
                            .clickable {
                                backgroundCoroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        Desktop.getDesktop()
                                            .browse(URI.create("https://twitchtokengenerator.com/"))
                                    }
                                }
                            }
                            .pointerHoverIcon(PointerIcon.Hand),
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colors.primary
                    )
                }

                Row (
                    modifier = Modifier
                        .padding(top = 5.dp)
                ) {
                    TextField(
                        label = {
                            Text(
                                text = "Twitch channel"
                            )
                        },
                        value = channel,
                        onValueChange = {
                            channel = it
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                Row (
                    modifier = Modifier
                        .padding(top = 10.dp)
                ) {
                    TextField(
                        label = {
                            Text(
                                text = "Spotify Client ID"
                            )
                        },
                        value = spotifyId,
                        onValueChange = {
                            spotifyId = it
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                Row (
                    modifier = Modifier
                        .padding(top = 5.dp)
                ) {
                    TextField(
                        label = {
                            Text(
                                text = "Spotify Client Secret"
                            )
                        },
                        value = spotifySecret,
                        onValueChange = {
                            spotifySecret = it
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp)
                ) {
                    Text(
                        text = "Get spotify ID and secret from here: "
                    )

                    Text(
                        style = MaterialTheme.typography.body1,
                        text = "developer.spotify.com",
                        modifier = Modifier
                            .clickable {
                                backgroundCoroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        Desktop.getDesktop()
                                            .browse(URI.create("https://developer.spotify.com/dashboard"))
                                    }
                                }
                            }
                            .pointerHoverIcon(PointerIcon.Hand),
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colors.primary
                    )
                }

                Row (
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Button(
                        onClick = {
                            backgroundCoroutineScope.launch {
                                isSetupInProgress = true
                                setupProject(twitchToken, channel, spotifyId, spotifySecret)
                                delay(1.seconds)
                                isSetupInProgress = false

                                scaffoldState.snackbarHostState.showSnackbar(
                                    message = "Project structure has been built! You can close this app now",
                                    duration = SnackbarDuration.Long
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Bottom),
                        enabled = !isSetupInProgress
                    ) {
                        Text("Setup project")
                    }
                }
            }
        }
    }
}


fun setupProject(twitchToken: String, channel: String, spotifyId: String, spotifySecret: String) {
    val dataFolderPath = "data"
    val tokensFolderPath = "$dataFolderPath\\tokens"
    val propertiesFolderPath = "$dataFolderPath\\properties"
    val folders = listOf(
        File(dataFolderPath),
        File(tokensFolderPath),
        File(propertiesFolderPath)
    )

    folders.forEach { folder ->
        if(!folder.exists() || !folder.isDirectory) {
            folder.mkdir()
        }
    }

    val spotifyClientSecretFileName = "spotifyClientSecret.txt"
    val twitchTokenFileName = "twitchToken.txt"
    val spotifyConfigFileName = "spotifyConfig.properties"
    val twitchBotConfigFileName = "twitchBotConfig.properties"

    val foldersToConfigFiles = mapOf(
        tokensFolderPath to listOf(
            spotifyClientSecretFileName,
            twitchTokenFileName
        ),
        propertiesFolderPath to listOf(
            spotifyConfigFileName,
            twitchBotConfigFileName,
            "botConfig.properties"
        )
    )

    foldersToConfigFiles.forEach { (folderPath, fileNames) ->
        fileNames.forEach { fileName ->
            val file = File(folderPath + "\\" + fileName)

            if(!file.exists()) {
                file.createNewFile()

                var content = ""
                if(file.name == spotifyClientSecretFileName) {
                    content = spotifySecret
                }

                if(file.name == twitchTokenFileName) {
                    content = twitchToken
                }

                if(file.name == spotifyConfigFileName) {
                    content = "spotifyClientId=$spotifyId"
                }

                if(file.name == twitchBotConfigFileName) {
                    content = "channel=$channel"
                }

                if(content != "") {
                    file.writeText(content)
                }
            }
        }
    }
}