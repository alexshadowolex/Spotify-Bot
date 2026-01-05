package commands

import config.BotConfig
import config.TwitchBotConfig
import deviceId
import getCurrentSpotifySong
import handleCommandSanityChecksWithSecurityLevel
import handler.Command
import httpClient
import io.ktor.client.request.*
import io.ktor.http.*
import isUserEligibleForPauseResumeCommand
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import spotifyClient

val pauseResumeCommand: Command = Command(
    commandDisplayName = "Pause/Resume Command",
    names = listOf("pauseresume", "pr", "pause", "resume", "play"),
    handler = {

        if(!handleCommandSanityChecksWithSecurityLevel(
                commandName = "pauseResumeCommand",
                isCommandEnabledFlag= BotConfig.isPauseResumeCommandEnabled,
                messageEvent = messageEvent,
                twitchClient = twitchClient,
                securityCheckFunction = ::isUserEligibleForPauseResumeCommand,
                securityLevel = BotConfig.pauseResumeCommandSecurityLevel,
            )) {
            return@Command
        }

        val isPlayerActive = getCurrentSpotifySong() != null
        val spotifyPlayer = spotifyClient.player
        val endpoint = "https://api.spotify.com/v1/me/player/"

        val result = if(isPlayerActive) {
            //spotifyPlayer.pause()
            // works
            httpClient.put(endpoint + "pause") {
                header("Authorization", "Bearer ${spotifyClient.token.accessToken}")
            }
        } else {
            //spotifyPlayer.resume()
            // does not work
            httpClient.post(endpoint + "play") {
                header("Authorization", "Bearer ${spotifyClient.token.accessToken}")
                parameter("device_id", deviceId)
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString<Temp>(Temp("spotify:album:5ht7ItJgpBH7W6vJ5BqpPr", T(5), 0)))
            }
        }

        println(result.status)
        println(result.headers)


        addedCommandCoolDown = TwitchBotConfig.defaultCommandCoolDownSeconds
    }
)



data class T(
    val position: Int
)

data class Temp(
    val context_uri: String,
    val offset: T,
    val position_ms: Int
)