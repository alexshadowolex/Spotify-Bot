package config

import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.seconds

object TwitchBotConfig {
    private val properties = Properties().apply {
        load(File("data\\properties\\twitchBotConfig.properties").inputStream())
    }

    val chatAccountToken = File("data\\tokens\\twitchToken.txt").readText()
    val channel: String = properties.getProperty("channel")
    val commandPrefix: String = properties.getProperty("command_prefix")
    val defaultCommandCoolDown = properties.getProperty("default_command_cool_down").toInt().seconds
    val defaultUserCoolDown = properties.getProperty("default_user_cool_down").toInt().seconds
    val songRequestRedeemId: String = properties.getProperty("song_request_redeem_id")
    val songRequestEmotes: List<String> = properties.getProperty("song_request_emotes").split(",")
    val isSongRequestCommandEnabledByDefault = properties.getProperty("is_song_request_command_enabled_by_default").toBoolean()
    val blacklistedUsers = properties.getProperty("blacklisted_users").split(",")
    val blacklistEmote: String = properties.getProperty("blacklist_emote")
    val isSpotifySongNameGetterEnabledByDefault = properties.getProperty("is_spotify_song_name_getter_enabled_by_default").toBoolean()
    val showNewVersionAvailableWindowOnStartUp = properties.getProperty("show_new_version_available_window_on_start_up").toBoolean()
    val isSongRequestEnabledByDefault = properties.getProperty("is_song_request_enabled_by_default").toBoolean()
    val isSongInfoCommandEnabledByDefault = properties.getProperty("is_song_info_command_enabled_by_default").toBoolean()
}