package redeems

import Config.TwitchBotConfig
import Redeem

val songRequestRedeem: Redeem = Redeem(
    id = TwitchBotConfig.songRequestRedeemId,
    handler = {

    }
)