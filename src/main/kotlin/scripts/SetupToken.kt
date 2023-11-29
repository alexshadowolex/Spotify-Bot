package scripts

import SpotifyConfig
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import config.TwitchBotConfig
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
            state = WindowState(size = DpSize(600.dp, 350.dp)),
            title = "Setup Token",
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
    MaterialTheme {
        Scaffold {
            Column(
                modifier = Modifier.fillMaxWidth()
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
                Row {
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
                                    JOptionPane.showMessageDialog(null, e.message + "\n" + StringWriter().also { e.printStackTrace(PrintWriter(it)) }, "InfoBox: File Debugger", JOptionPane.INFORMATION_MESSAGE)
                                    exitProcess(0)
                                }


                                File("data\\tokens\\spotifyToken.json").writeText(Json.encodeToString(api.token))
                                JOptionPane.showMessageDialog(null, "Success! You can close this App now!", "Success", JOptionPane.INFORMATION_MESSAGE)
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