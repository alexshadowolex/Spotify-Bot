package config

import getPropertyValue
import joinToPropertiesString
import logger
import showErrorMessageWindow
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

object TwitchBotConfig {
    private val twitchBotConfigFile = File("data\\properties\\twitchBotConfig.properties")
    private val properties = Properties().apply {
        if(!twitchBotConfigFile.exists()) {
            logger.error(
                "Error while reading property file ${twitchBotConfigFile.path} in TwitchBotConfig init: " +
                "File does not exist!"
            )
            showErrorMessageWindow(
                title = "Error while reading properties file",
                message = "Property file \"${twitchBotConfigFile.path}\" does not exist!"
            )

            exitProcess(-1)
        }
        load(twitchBotConfigFile.inputStream())
    }

    val chatAccountToken = File("data\\tokens\\twitchToken.txt").readText()
    val channel: String = getPropertyValue(properties, "channel", twitchBotConfigFile.path)
    var commandPrefix: String = getPropertyValue(properties, "command_prefix", twitchBotConfigFile.path)
        set(value) {
            field = value
            properties.setProperty("command_prefix", value)
            savePropertiesToFile()
        }
    var defaultCommandCoolDown = getPropertyValue(
        properties, "default_command_cool_down", twitchBotConfigFile.path
    ).toInt().seconds
        set(value) {
            field = value
            properties.setProperty("default_command_cool_down", value.toInt(DurationUnit.SECONDS).toString())
            savePropertiesToFile()
        }
    var defaultUserCoolDown = getPropertyValue(
        properties, "default_user_cool_down", twitchBotConfigFile.path
    ).toInt().seconds
        set(value) {
            field = value
            properties.setProperty("default_user_cool_down", value.toInt(DurationUnit.SECONDS).toString())
            savePropertiesToFile()
        }
    val songRequestRedeemId: String = getPropertyValue(
        properties, "song_request_redeem_id", twitchBotConfigFile.path
    )
    var songRequestEmotes: List<String> = getPropertyValue(
        properties, "song_request_emotes", twitchBotConfigFile.path
    ).split(",")
        set(value) {
            field = value
            properties.setProperty("song_request_emotes", value.joinToPropertiesString(","))
            savePropertiesToFile()
        }
    var isSongRequestCommandEnabledByDefault = getPropertyValue(
        properties, "is_song_request_command_enabled_by_default", twitchBotConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("is_song_request_command_enabled_by_default", value.toString())
            savePropertiesToFile()
        }
    var blacklistedUsers = getPropertyValue(
        properties, "blacklisted_users", twitchBotConfigFile.path
    ).split(",")
        set(value) {
            field = value
            properties.setProperty("blacklisted_users", value.joinToPropertiesString(","))
            savePropertiesToFile()
        }
    var blacklistEmote: String = getPropertyValue(properties, "blacklist_emote", twitchBotConfigFile.path)
        set(value) {
            field = value
            properties.setProperty("blacklist_emote", value)
            savePropertiesToFile()
        }
    var isSpotifySongNameGetterEnabledByDefault = getPropertyValue(
        properties, "is_spotify_song_name_getter_enabled_by_default", twitchBotConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("is_spotify_song_name_getter_enabled_by_default", value.toString())
            savePropertiesToFile()
        }
    var showNewVersionAvailableWindowOnStartUp = getPropertyValue(
        properties, "show_new_version_available_window_on_start_up", twitchBotConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("show_new_version_available_window_on_start_up", value.toString())
            savePropertiesToFile()
        }
    var isSongRequestEnabledByDefault = getPropertyValue(
        properties, "is_song_request_enabled_by_default", twitchBotConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("is_song_request_enabled_by_default", value.toString())
            savePropertiesToFile()
        }
    var isSongInfoCommandEnabledByDefault = getPropertyValue(
        properties, "is_song_info_command_enabled_by_default", twitchBotConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("is_song_info_command_enabled_by_default", value.toString())
            savePropertiesToFile()
        }
    var isEmptySongDisplayFilesOnPauseEnabledByDefault = getPropertyValue(
        properties, "is_empty_song_display_files_on_pause_enabled_by_default", twitchBotConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("is_empty_song_display_files_on_pause_enabled_by_default", value.toString())
            savePropertiesToFile()
        }
    var isAddSongCommandEnabledByDefault = getPropertyValue(
        properties, "is_add_song_command_enabled_by_default", twitchBotConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("is_add_song_command_enabled_by_default", value.toString())
            savePropertiesToFile()
        }
    var isSkipSongCommandEnabledByDefault = getPropertyValue(
        properties, "is_skip_song_command_enabled_by_default", twitchBotConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("is_skip_song_command_enabled_by_default", value.toString())
            savePropertiesToFile()
        }
    var isRemoveSongFromQueueCommandEnabledByDefault = getPropertyValue(
        properties, "is_remove_song_from_queue_command_enabled_by_default", twitchBotConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("is_remove_song_from_queue_command_enabled_by_default", value.toString())
            savePropertiesToFile()
        }


    private fun savePropertiesToFile() {
        properties.store(FileOutputStream(twitchBotConfigFile.path), null)
    }
}