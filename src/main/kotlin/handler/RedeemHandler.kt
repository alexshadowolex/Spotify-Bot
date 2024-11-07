package handler

import com.github.twitch4j.TwitchClient
import com.github.twitch4j.chat.TwitchChat
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent
import redeems.songRequestRedeem

data class Redeem(
    var id: String, // Can initially hold the name of the redeem
    val handler: suspend RedeemHandlerScope.(arguments: String) -> Unit
)

data class RedeemHandlerScope(
    val twitchClient: TwitchClient,
    val redeemEvent: RewardRedeemedEvent
)

val redeems = listOf(
    songRequestRedeem
)