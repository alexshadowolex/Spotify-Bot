package scripts

import SpotifyConfig
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.adamratzman.spotify.SpotifyScope
import com.adamratzman.spotify.getSpotifyAuthorizationUrl
import com.adamratzman.spotify.spotifyClientApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URI
import javax.swing.JOptionPane
import kotlin.system.exitProcess

// Current Version: v3

fun main() = try {
    val redirectUri = "https://www.example.com"

    Desktop.getDesktop().browse(
        URI.create(
            getSpotifyAuthorizationUrl(
                scopes = SpotifyScope.values(),
                clientId = SpotifyConfig.spotifyClientId,
                redirectUri = redirectUri
            )
        )
    )

    application {

        Window(
            state = WindowState(size = DpSize(600.dp, 200.dp)),
            title = "Setup Token",
            resizable = false,
            onCloseRequest = ::exitApplication,
        ) {
            app2(redirectUri)
        }
    }
} catch (e: Exception) {
    JOptionPane.showMessageDialog(null, e.message + "\n" + StringWriter().also { e.printStackTrace(PrintWriter(it)) }, "InfoBox: File Debugger", JOptionPane.INFORMATION_MESSAGE)
    exitProcess(0)
}


@Composable
fun app2(redirectUri: String) {
    var spotifyCode by remember { mutableStateOf("") }
    val scaffoldState = rememberScaffoldState()
    MaterialTheme {
        Scaffold (
            scaffoldState = scaffoldState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Row {
                    TextField(
                        label = {
                            Text(
                                text = "Spotify Redirect Code"
                            )
                        },
                        value = spotifyCode,
                        onValueChange = {
                            spotifyCode = it
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
                    Text(
                        text = "A browser tab should have opened from example.com. " +
                                "Copy the code from the URL behind \"?code=\" and paste it into the text field."
                    )
                }

                Row (
                    modifier = Modifier
                        .padding(top = 5.dp)
                ) {
                    Button(
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                val api = try {
                                    spotifyClientApi {
                                        credentials {
                                            clientId = SpotifyConfig.spotifyClientId
                                            clientSecret = SpotifyConfig.spotifyClientSecret
                                            this.redirectUri = redirectUri
                                        }

                                        authorization {
                                            authorizationCode = spotifyCode
                                        }
                                    }.build()
                                } catch (e: Exception) {
                                    JOptionPane.showMessageDialog(
                                        null,
                                        "An error occurred. Make sure you give the correct code. Message: " +
                                                e.message + "\n" +
                                                StringWriter().also { e.printStackTrace(PrintWriter(it)) },
                                        "InfoBox: File Debugger",
                                        JOptionPane.ERROR_MESSAGE
                                    )
                                    null
                                }

                                if(api != null) {
                                    File("data\\tokens\\spotifyToken.json").writeText(Json.encodeToString(api.token))
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        message = "Token setup successfully! You can close this app now",
                                        duration = SnackbarDuration.Long
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Text("Generate Spotify Token")
                    }
                }
            }
        }
    }
}