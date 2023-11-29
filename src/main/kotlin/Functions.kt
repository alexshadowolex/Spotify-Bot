import com.adamratzman.spotify.endpoints.pub.SearchApi
import com.adamratzman.spotify.models.Track
import com.adamratzman.spotify.utils.Market
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.TwitchClient
import com.github.twitch4j.TwitchClientBuilder
import com.github.twitch4j.chat.TwitchChat
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent
import config.TwitchBotConfig
import handler.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths
import java.time.format.DateTimeFormatterBuilder
import kotlin.time.Duration.Companion.seconds

suspend fun setupTwitchBot(): TwitchClient {
    val oAuth2Credential = OAuth2Credential("twitch", TwitchBotConfig.chatAccountToken)

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

    val nextAllowedCommandUsageInstantPerUser = mutableMapOf<Pair<Command, /* user: */ String>, Instant>()
    val nextAllowedCommandUsageInstantPerCommand = mutableMapOf<Command, Instant>()

    val channelId = twitchClient.helix.getUsers(TwitchBotConfig.chatAccountToken, null, listOf(TwitchBotConfig.channel)).execute().users.first().id
    twitchClient.pubSub.listenForChannelPointsRedemptionEvents(
        oAuth2Credential,
        channelId
    )

    twitchClient.pubSub.listenForChannelPointsRedemptionEvents(oAuth2Credential, channelId)

    twitchClient.eventManager.onEvent(RewardRedeemedEvent::class.java) { rewardRedeemEvent ->
        rewardRedeemEventHandler(rewardRedeemEvent, twitchClient)
    }

    twitchClient.eventManager.onEvent(ChannelMessageEvent::class.java) { messageEvent ->
        val message = messageEvent.message
        if (!message.startsWith(TwitchBotConfig.commandPrefix)) {
            return@onEvent
        }

        val parts = message.substringAfter(TwitchBotConfig.commandPrefix).split(" ")

        val command = commands.find { parts.first().substringAfter(TwitchBotConfig.commandPrefix).lowercase() in it.names } ?: return@onEvent

        logger.info("User '${messageEvent.user.name}' tried using command '${command.names.first()}' with arguments: ${parts.drop(1).joinToString()}")

        if(isUserBlacklisted(messageEvent.user.name, messageEvent.user.id, twitchClient.chat)) {
            return@onEvent
        }

        val nextAllowedCommandUsageInstant = nextAllowedCommandUsageInstantPerCommand.getOrPut(command) {
            Clock.System.now()
        }

        val nextAllowedCommandUsageInstantForUser = nextAllowedCommandUsageInstantPerUser.getOrPut(command to messageEvent.user.name) {
            Clock.System.now()
        }
        if((Clock.System.now() - nextAllowedCommandUsageInstant).isNegative() && messageEvent.user.name != TwitchBotConfig.channel) {
            val secondsUntilTimeoutOver = (nextAllowedCommandUsageInstant - Clock.System.now()).inWholeSeconds.seconds

            twitchClient.chat.sendMessage(TwitchBotConfig.channel, "Command is still on cooldown. Please try again in $secondsUntilTimeoutOver")

            return@onEvent
        }

        if ((Clock.System.now() - nextAllowedCommandUsageInstantForUser).isNegative() && messageEvent.user.name != TwitchBotConfig.channel) {
            val secondsUntilTimeoutOver = (nextAllowedCommandUsageInstantForUser - Clock.System.now()).inWholeSeconds.seconds

            twitchClient.chat.sendMessage(TwitchBotConfig.channel, "You are still on cooldown. Please try again in $secondsUntilTimeoutOver")

            return@onEvent
        }

        val commandHandlerScope = CommandHandlerScope(
            chat = twitchClient.chat,
            messageEvent = messageEvent,
        )

        backgroundCoroutineScope.launch {
            command.handler(commandHandlerScope, parts.drop(1))

            val key = command to messageEvent.user.name
            nextAllowedCommandUsageInstantPerUser[key] = Clock.System.now() + commandHandlerScope.addedUserCoolDown

            nextAllowedCommandUsageInstantPerCommand[command] = Clock.System.now() + commandHandlerScope.addedCommandCoolDown
        }
    }

    logger.info("Twitch client started.")
    return twitchClient
}

fun rewardRedeemEventHandler(redeemEvent: RewardRedeemedEvent, twitchClient: TwitchClient) {
    val redeem = redeems.find { redeemEvent.redemption.reward.id in it.id || redeemEvent.redemption.reward.title in it.id }.also {
        if (it != null) {
            if(redeemEvent.redemption.reward.title in it.id) {
                logger.warn("Redeem ${redeemEvent.redemption.reward.title}. Please use following ID in the properties file instead of the name: ${redeemEvent.redemption.reward.id}")
            }
        }
    } ?: return

    if(isUserBlacklisted(redeemEvent.redemption.user.displayName, redeemEvent.redemption.user.id, twitchClient.chat)) {
        return
    }

    val redeemHandlerScope = RedeemHandlerScope(
        chat = twitchClient.chat,
        redeemEvent = redeemEvent
    )

    backgroundCoroutineScope.launch {
        redeem.handler(redeemHandlerScope, redeemEvent.redemption.userInput)
    }
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

fun isUserBlacklisted(userName: String, userId: String, chat: TwitchChat): Boolean {

    if(userName in TwitchBotConfig.blacklistedUsers || userId in TwitchBotConfig.blacklistedUsers){

        chat.sendMessage(TwitchBotConfig.channel, "Imagine not being a blacklisted user. Couldn't be you $userName ${TwitchBotConfig.blacklistEmote}")
        if(userId !in TwitchBotConfig.blacklistedUsers) {
            logger.warn("Blacklisted user $userName tried using a command. Please use following ID in the properties file instead of the name: $userId")
        }
        return true
    }
    return false
}

suspend fun handleSongRequestQuery(chat: TwitchChat, query: String): Boolean {
    var success = true
    try {
        chat.sendMessage(
            TwitchBotConfig.channel,
            updateQueue(query).let { response ->
                val track = response.track
                if(track != null) {
                    "Song '${track.name}' by ${
                        track.artists.map { "'${it.name}'" }.let { artists ->
                            listOf(
                                artists.dropLast(1).joinToString(),
                                artists.last()
                            ).filter { it.isNotBlank() }.joinToString(" and ")
                        }
                    } has been added to the playlist ${TwitchBotConfig.songRequestEmotes.random()}"
                } else {
                    success = false
                    "Couldn't add song to the queue. ${response.songRequestResultExplanation}"
                }
            }
        )
    } catch (e: Exception) {
        logger.error("Something went wrong with songrequests", e)
        success = false
    }

    return success
}

private suspend fun updateQueue(query: String): SongRequestResult {
    val result = try {
        Url(query).takeIf { it.host == "open.spotify.com" && it.encodedPath.contains("/track/") }?.let {
            val songId = it.encodedPath.substringAfter("/track/")
            logger.info("Song ID from link: $songId")
            spotifyClient.tracks.getTrack(
                track = songId,
                market = Market.DE
            )
        } ?: run {
            spotifyClient.search.search(
                query = query,
                searchTypes = arrayOf(
                    SearchApi.SearchType.Artist,
                    SearchApi.SearchType.Album,
                    SearchApi.SearchType.Track
                ),
                market = Market.DE
            ).tracks?.firstOrNull()
        } ?: return SongRequestResult(
            track = null,
            songRequestResultExplanation = "No Result when searching for song."
        )
    } catch (e: Exception) {
        logger.error("Error while searching for track:", e)
        return SongRequestResult(
            track = null,
            songRequestResultExplanation = "Exception when accessing spotify endpoints for searching the song."
        )
    }

    logger.info("Result after search: $result")

    try {
        spotifyClient.player.addItemToEndOfQueue(result.uri)
        logger.info("Result URI: ${result.uri.uri}")
    } catch (e: Exception) {
        logger.error("Spotify is probably not set up.", e)
        return SongRequestResult(
            track = null,
            songRequestResultExplanation = "Adding the song to the playlist failed."
        )
    }

    return SongRequestResult(
        track = result,
        songRequestResultExplanation = "Successfully added the song to the playlist."
    )
}

private data class SongRequestResult(
    val track: Track?,
    val songRequestResultExplanation: String
)