import com.github.twitch4j.chat.TwitchChat
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent
import redeems.songRequestRedeem

data class Redeem(
    val id: String, // Will initially hold the name of the redeem
    val handler: suspend RedeemHandlerScope.(arguments: String) -> Unit
)

data class RedeemHandlerScope(
    val chat: TwitchChat,
    val redeemEvent: RewardRedeemedEvent
)

val redeems = listOf(
    songRequestRedeem
)