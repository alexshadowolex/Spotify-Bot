import config.TwitchBotConfig
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.adamratzman.spotify.SpotifyClientApi
import com.adamratzman.spotify.models.Token
import com.adamratzman.spotify.spotifyClientApi
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.TwitchClient
import com.github.twitch4j.TwitchClientBuilder
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.time.format.DateTimeFormatterBuilder
import javax.swing.JOptionPane
import kotlin.system.exitProcess

val logger: org.slf4j.Logger = LoggerFactory.getLogger("Bot")

val backgroundCoroutineScope = CoroutineScope(Dispatchers.IO)

lateinit var spotifyClient: SpotifyClientApi

val httpClient = HttpClient(CIO) {
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.INFO
    }

    install(ContentNegotiation) {
        json()
    }
}
suspend fun main() = try {
    setupLogging()
    val twitchClient = setupTwitchBot()
    val initialToken: Token = Json.decodeFromString(File("data/spotifytoken.json").readText())

    application {
        DisposableEffect(Unit) {
            spotifyClient = runBlocking {
                spotifyClientApi(
                    clientId = TwitchBotConfig.spotifyClientId,
                    clientSecret = TwitchBotConfig.spotifyClientSecret,
                    redirectUri = "https://www.example.com",
                    token = initialToken
                ) {
                    onTokenRefresh = {
                        logger.info("Token refreshed")
                    }
                    afterTokenRefresh = {
                        it.token.refreshToken = initialToken.refreshToken
                        try {
                            File("data/spotifytoken.json").writeText(json.encodeToString(it.token.copy(refreshToken = initialToken.refreshToken)))
                        } catch(e: Exception) {
                            logger.error("Error occured while saving new token", e)
                        }
                    }
                    enableLogger = true
                }.build()
            }

            logger.info("Spotify client built successfully.")

            onDispose {
                twitchClient.chat.sendMessage(TwitchBotConfig.channel, "Bot shutting down peepoLeave")
                logger.info("App shutting down...")
            }
        }
        Window(
            state = WindowState(size = DpSize(350.dp, 250.dp)),
            title = "SovereignsBot",
            onCloseRequest = ::exitApplication,
        ) {
            App()
        }
    }
} catch (e: Throwable) {
    JOptionPane.showMessageDialog(null, e.message + "\n" + StringWriter().also { e.printStackTrace(PrintWriter(it)) }, "InfoBox: File Debugger", JOptionPane.INFORMATION_MESSAGE)
    logger.error("Error while executing program.", e)
    exitProcess(0)
}

private suspend fun setupTwitchBot(): TwitchClient {
    val chatAccountToken = File("data/twitchtoken.txt").readText()
    val oAuth2Credential = OAuth2Credential("twitch", chatAccountToken)

    val twitchClient = TwitchClientBuilder.builder()
        .withEnableHelix(true)
        .withEnableChat(true)
        .withEnablePubSub(true)
        .withChatAccount(oAuth2Credential)
        .build()

    twitchClient.chat.run {
        connect()
        joinChannel(TwitchBotConfig.channel)
        sendMessage(TwitchBotConfig.channel, "Bot running peepoArrive")
    }

    val channelId = twitchClient.helix.getUsers(chatAccountToken, null, listOf(TwitchBotConfig.channel)).execute().users.first().id
    twitchClient.pubSub.listenForChannelPointsRedemptionEvents(
        oAuth2Credential,
        channelId
    )

    twitchClient.pubSub.listenForChannelPointsRedemptionEvents(oAuth2Credential, channelId)

    twitchClient.eventManager.onEvent(RewardRedeemedEvent::class.java) { redeemEvent ->

        val redeem = redeems.find { redeemEvent.redemption.reward.id in it.id || redeemEvent.redemption.reward.title in it.id }.also {
            if (it != null) {
                if(redeemEvent.redemption.reward.title in it.id) {
                    logger.warn("Redeem ${redeemEvent.redemption.reward.title}. Please use following ID in the properties file instead of the name: ${redeemEvent.redemption.reward.id}")
                }
            }
        } ?: return@onEvent

        val redeemHandlerScope = RedeemHandlerScope(
            chat = twitchClient.chat,
            redeemEvent = redeemEvent
        )

        backgroundCoroutineScope.launch {
            redeem.handler(redeemHandlerScope, redeemEvent.redemption.userInput)
        }
    }

    logger.info("Twitch client started.")
    return twitchClient
}

private const val LOG_DIRECTORY = "logs"

fun setupLogging() {
    Files.createDirectories(Paths.get(LOG_DIRECTORY))

    val logFileName = DateTimeFormatterBuilder()
        .appendInstant(0)
        .toFormatter()
        .format(Clock.System.now().toJavaInstant())
        .replace(':', '-')

    val logFile = Paths.get(LOG_DIRECTORY, "${logFileName}.log").toFile().also {
        if (!it.exists()) {
            it.createNewFile()
        }
    }

    System.setOut(PrintStream(MultiOutputStream(System.out, FileOutputStream(logFile))))

    logger.info("Log file '${logFile.name}' has been created.")
}