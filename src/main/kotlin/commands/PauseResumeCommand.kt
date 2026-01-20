package commands

import config.BotConfig
import config.CacheConfig
import getCurrentDeviceId
import handleCommandSanityChecksWithSecurityLevel
import handler.Command
import httpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import isSpotifyPlaying
import isUserEligibleForPauseResumeCommand
import logger
import sendMessageToTwitchChatAndLogIt
import spotifyClient
import kotlin.time.Duration.Companion.seconds

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

        val isPlayerActive = isSpotifyPlaying() == true
        var deviceId = resolveDeviceId()
        val httpResultSuccessStatus = 200

        val initialResult = toggleSpotifyPlayback(isPlayerActive, deviceId)

        val success = if (initialResult.status.value == httpResultSuccessStatus) {
            true
        } else {
            logger.info("First toggle was not finished successfully, retrying with new device ID")
            logger.error("Initial device ID: " + if (deviceId == null || deviceId == "") {
                "was null or empty"
            } else {
                "seemed valid (not null and not empty"
            })
            logger.error("Initial result body: ${initialResult.bodyAsText()}")
            deviceId = getCurrentDeviceId()
            CacheConfig.spotifyDeviceId = deviceId

            val retryResult = toggleSpotifyPlayback(isPlayerActive, deviceId)

            (deviceId != null && retryResult.status.value == httpResultSuccessStatus).also {
                if (retryResult.status.value != httpResultSuccessStatus) {
                    logger.error("Second toggle also did not work")
                    logger.error("Retry device ID: " + if (deviceId == null || deviceId == "") {
                        "was null or empty"
                    } else {
                        "seemed valid (not null and not empty)"
                    })
                    logger.error("Retry result body: ${retryResult.bodyAsText()}")
                }
            }
        }

        val message = if (success) {
            if (isPlayerActive) {
                "Paused playback successfully"
            } else {
                "Resumed playback successfully"
            }
        } else {
            if (isPlayerActive) {
                "Pausing playback failed"
            } else {
                "Resuming playback failed"
            }
        }

        sendMessageToTwitchChatAndLogIt(twitchClient.chat, message)

        addedCommandCoolDown = 5.seconds
    }
)


/**
 * Resolves the current Spotify device ID.
 *
 * This function first attempts to return a cached device ID from [CacheConfig.spotifyDeviceId].
 * If no cached value is present, it fetches the current device ID via [getCurrentDeviceId],
 * caches the result, and returns it.
 *
 * @return the resolved Spotify device ID, or `null` if no active device is available
 */
private suspend fun resolveDeviceId(): String? {
    val cached = CacheConfig.spotifyDeviceId
    if (cached != null) return cached

    val fresh = getCurrentDeviceId()
    CacheConfig.spotifyDeviceId = fresh
    return fresh
}


/**
 * Toggles Spotify playback state for the current user.
 *
 * If the player is currently active, this function pauses playback; otherwise, it
 * starts or resumes playback. An optional device ID may be supplied to target
 * a specific playback device.
 *
 * @param isPlayerActive `true` if playback is currently active and should be paused,
 * `false` if playback is inactive and should be started
 * @param deviceId optional Spotify device ID; if provided, playback is controlled
 * on that specific device
 * @return the [HttpResponse] returned by the Spotify Web API
 */
private suspend fun toggleSpotifyPlayback(isPlayerActive: Boolean, deviceId: String?): HttpResponse {
    val endpoint = "https://api.spotify.com/v1/me/player/"
    val deviceIdString = deviceId?.let { "?device_id=$deviceId" } ?: ""

    return if(isPlayerActive) {
        httpClient.put(endpoint + "pause$deviceIdString") {
            header("Authorization", "Bearer ${spotifyClient.token.accessToken}")
        }
    } else {
        httpClient.put(endpoint + "play$deviceIdString") {
            header("Authorization", "Bearer ${spotifyClient.token.accessToken}")
        }
    }
}