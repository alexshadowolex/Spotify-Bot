import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.adamratzman.spotify.SpotifyClientApi
import com.adamratzman.spotify.models.Token
import com.adamratzman.spotify.spotifyClientApi
import config.TwitchBotConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import ui.app
import ui.newVersionScreen
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.JOptionPane
import kotlin.system.exitProcess

val logger: org.slf4j.Logger = LoggerFactory.getLogger("Bot")

val backgroundCoroutineScope = CoroutineScope(Dispatchers.IO)

lateinit var spotifyClient: SpotifyClientApi

suspend fun main() = try {
    setupLogging()
    val twitchClient = setupTwitchBot()
    val initialToken: Token = Json.decodeFromString(File("data\\tokens\\spotifyToken.json").readText())

    application {
        DisposableEffect(Unit) {
            spotifyClient = runBlocking {
                spotifyClientApi(
                    clientId = SpotifyConfig.spotifyClientId,
                    clientSecret = SpotifyConfig.spotifyClientSecret,
                    redirectUri = "https://www.example.com",
                    token = initialToken
                ) {
                    onTokenRefresh = {
                        logger.info("Token refreshed")
                    }
                    afterTokenRefresh = {
                        it.token.refreshToken = initialToken.refreshToken
                        try {
                            File("data\\tokens\\spotifyToken.json").writeText(json.encodeToString(it.token.copy(refreshToken = initialToken.refreshToken)))
                        } catch(e: Exception) {
                            logger.error("Error occured while saving new token", e)
                        }
                    }
                    enableLogger = true
                }.build()
            }

            logger.info("Spotify client built successfully.")

            startSpotifySongNameGetter()

            onDispose {
                twitchClient.chat.sendMessage(TwitchBotConfig.channel, "Bot shutting down peepoLeave")
                logger.info("App shutting down...")
            }
        }

        Window(
            state = WindowState(size = DpSize(350.dp, 250.dp)),
            resizable = false,
            title = "Spotify Bot",
            onCloseRequest = ::exitApplication,
            icon = painterResource("logo.png")
        ) {
            app()
        }

        if (TwitchBotConfig.showNewVersionAvailableWindowOnStartUp && isNewAppReleaseAvailable()) {
            val isNewVersionWindowOpen = remember{ mutableStateOf(true) }
            if(isNewVersionWindowOpen.value) {
                Window(
                    state = WindowState(size = DpSize(500.dp, 150.dp)),
                    resizable = false,
                    title = "New Version Available!",
                    onCloseRequest = {
                        isNewVersionWindowOpen.value = false
                    },
                    icon = painterResource("logo.png")
                ) {
                    window.requestFocus()
                    newVersionScreen(isNewVersionWindowOpen)
                }
            }
        }
    }

} catch (e: Throwable) {
    JOptionPane.showMessageDialog(null, e.message + "\n" + StringWriter().also { e.printStackTrace(PrintWriter(it)) }, "InfoBox: File Debugger", JOptionPane.INFORMATION_MESSAGE)
    logger.error("Error while executing program.", e)
    exitProcess(0)
}