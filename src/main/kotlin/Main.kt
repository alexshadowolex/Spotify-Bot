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
import config.BotConfig
import config.SpotifyConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import ui.Screen
import ui.app
import ui.screens.newVersionScreen
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

val logger: org.slf4j.Logger = LoggerFactory.getLogger("Bot")

val backgroundCoroutineScope = CoroutineScope(Dispatchers.IO)

val httpClient = HttpClient(CIO) {
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.NONE
    }

    install(ContentNegotiation) {
        json()
    }
}

lateinit var spotifyClient: SpotifyClientApi
var currentSongString = ""
var windowWidth = mutableStateOf(Screen.HomeScreen.width)
var windowHeight = mutableStateOf(Screen.HomeScreen.height)


suspend fun main() = try {
    setupLogging()
    val twitchClient = setupTwitchBot()
    val initialToken: Token = Json.decodeFromString(File("data\\tokens\\spotifyToken.json").readText())
    var alreadyCheckedNewVersion = false

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
                        logger.info("Spotify token refreshed")
                    }
                    afterTokenRefresh = {
                        it.token.refreshToken = initialToken.refreshToken
                        try {
                            File("data\\tokens\\spotifyToken.json").writeText(
                                json.encodeToString(it.token.copy(refreshToken = initialToken.refreshToken))
                            )
                        } catch(e: Exception) {
                            logger.error("Error occurred while saving new spotify token", e)
                        }
                    }
                }.build()
            }

            logger.info("Spotify client built successfully.")

            startSpotifySongGetter()

            onDispose {
                sendMessageToTwitchChatAndLogIt(twitchClient.chat, "Bot shutting down peepoLeave")
                if(BotConfig.isEmptySongDisplayFilesOnPauseEnabled && BotConfig.isSpotifySongNameGetterEnabled) {
                    emptyAllSongDisplayFiles()
                }
                logger.info("App shutting down...")
            }
        }

        Window(
            state = WindowState(size = DpSize(windowWidth.value, windowHeight.value)),
            resizable = false,
            title = "Spotify Bot",
            onCloseRequest = ::exitApplication,
            icon = painterResource("logo.png")
        ) {
            app()
        }

        if (BotConfig.isNewVersionCheckEnabled && !alreadyCheckedNewVersion) {
            alreadyCheckedNewVersion = true
            if(isNewAppReleaseAvailable()) {
                val isNewVersionWindowOpen = remember { mutableStateOf(true) }
                if (isNewVersionWindowOpen.value) {
                    Window(
                        state = WindowState(size = DpSize(500.dp, 200.dp)),
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
    }

} catch (e: Throwable) {
    showErrorMessageWindow(
        message =   e.message + "\n" +
                    StringWriter().also { e.printStackTrace(PrintWriter(it)) },
        title = "Error while executing app"
    )
    logger.error("Error while executing program. ", e)
    exitProcess(-1)
}