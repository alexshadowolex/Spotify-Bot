package handler

import com.github.twitch4j.TwitchClient
import com.github.twitch4j.chat.TwitchChat
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import commands.*
import kotlin.time.Duration

data class Command(
    val commandDisplayName: String,
    val names: List<String>,
    val handler: suspend CommandHandlerScope.(arguments: List<String>) -> Unit
)

data class CommandHandlerScope(
    val twitchClient: TwitchClient,
    val messageEvent: ChannelMessageEvent,
    val removeSongFromQueueHandler: RemoveSongFromQueueHandler,
    var addedUserCoolDown: Duration = Duration.ZERO,
    var addedCommandCoolDown: Duration = Duration.ZERO
)

val commands = listOf(
    songRequestCommand,
    songInfoCommand,
    addSongCommand,
    skipSongCommand,
    removeSongFromQueueCommand,
    blockSongCommand
)