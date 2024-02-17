package config

import getPropertyValue
import java.io.File
import java.util.*
import kotlin.time.Duration.Companion.seconds

object TwitchBotConfig {
    private val twitchBotConfigFile = File("data\\properties\\twitchBotConfig.properties")
    private val properties = Properties().apply {
        load(twitchBotConfigFile.inputStream())
    }

    val chatAccountToken = File("data\\tokens\\twitchToken.txt").readText()
    val channel: String = getPropertyValue(properties, "channel", twitchBotConfigFile.path)
    val commandPrefix: String = getPropertyValue(properties, "command_prefix", twitchBotConfigFile.path)
    val defaultCommandCoolDown = getPropertyValue(
        properties, "default_command_cool_down", twitchBotConfigFile.path
    ).toInt().seconds
    val defaultUserCoolDown = getPropertyValue(
        properties, "default_user_cool_down", twitchBotConfigFile.path
    ).toInt().seconds
    val songRequestRedeemId: String = getPropertyValue(
        properties, "song_request_redeem_id", twitchBotConfigFile.path
    )
    val songRequestEmotes: List<String> = getPropertyValue(
        properties, "song_request_emotes", twitchBotConfigFile.path
    ).split(",")
    val isSongRequestCommandEnabledByDefault = getPropertyValue(
        properties, "is_song_request_command_enabled_by_default", twitchBotConfigFile.path
    ).toBoolean()
    val blacklistedUsers = getPropertyValue(
        properties, "blacklisted_users", twitchBotConfigFile.path
    ).split(",")
    val blacklistEmote: String = getPropertyValue(properties, "blacklist_emote", twitchBotConfigFile.path)
    val isSpotifySongNameGetterEnabledByDefault = getPropertyValue(
        properties, "is_spotify_song_name_getter_enabled_by_default", twitchBotConfigFile.path
    ).toBoolean()
    val showNewVersionAvailableWindowOnStartUp = getPropertyValue(
        properties, "show_new_version_available_window_on_start_up", twitchBotConfigFile.path
    ).toBoolean()
    val isSongRequestEnabledByDefault = getPropertyValue(
        properties, "is_song_request_enabled_by_default", twitchBotConfigFile.path
    ).toBoolean()
    val isSongInfoCommandEnabledByDefault = getPropertyValue(
        properties, "is_song_info_command_enabled_by_default", twitchBotConfigFile.path
    ).toBoolean()
    val isEmptySongDisplayFilesOnPauseEnabledByDefault = getPropertyValue(
        properties, "is_empty_song_display_files_on_pause_enabled_by_default", twitchBotConfigFile.path
    ).toBoolean()
    val isAddSongCommandEnabledByDefault = getPropertyValue(
        properties, "is_add_song_command_enabled_by_default", twitchBotConfigFile.path
    ).toBoolean()
    val isSkipSongCommandEnabledByDefault = getPropertyValue(
        properties, "is_skip_song_command_enabled_by_default", twitchBotConfigFile.path
    ).toBoolean()
    val isRemoveSongFromQueueCommandEnabledByDefault = getPropertyValue(
        properties, "is_remove_song_from_queue_command_enabled_by_default", twitchBotConfigFile.path
    ).toBoolean()
}