package config

import CustomCommandPermissions
import displayEnumParsingErrorWindow
import getPropertyValue
import joinToPropertiesString
import logger
import showErrorMessageWindow
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.system.exitProcess

object BotConfig {
    private val botConfigFile = File("data\\properties\\botConfig.properties")
    private val properties = Properties().apply {
        if(!botConfigFile.exists()) {
            logger.error(
                "Error while reading property file ${botConfigFile.path} in TwitchBotConfig init: " +
                        "File does not exist!"
            )
            showErrorMessageWindow(
                title = "Error while reading properties file",
                message = "Property file \"${botConfigFile.path}\" does not exist!"
            )

            exitProcess(-1)
        }
        load(botConfigFile.inputStream())
    }


    var isSongRequestCommandEnabledByDefault = getPropertyValue(
        properties, "is_song_request_command_enabled_by_default", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("is_song_request_command_enabled_by_default", value.toString())
            savePropertiesToFile()
        }

    var blacklistedUsers = getPropertyValue(
        properties, "blacklisted_users", botConfigFile.path
    ).split(",")
        set(value) {
            field = value
            properties.setProperty("blacklisted_users", value.joinToPropertiesString(","))
            savePropertiesToFile()
        }

    var isSpotifySongNameGetterEnabledByDefault = getPropertyValue(
        properties, "is_spotify_song_name_getter_enabled_by_default", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("is_spotify_song_name_getter_enabled_by_default", value.toString())
            savePropertiesToFile()
        }

    var showNewVersionAvailableWindowOnStartUp = getPropertyValue(
        properties, "show_new_version_available_window_on_start_up", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("show_new_version_available_window_on_start_up", value.toString())
            savePropertiesToFile()
        }

    var isSongRequestEnabledByDefault = getPropertyValue(
        properties, "is_song_request_enabled_by_default", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("is_song_request_enabled_by_default", value.toString())
            savePropertiesToFile()
        }

    var isSongInfoCommandEnabledByDefault = getPropertyValue(
        properties, "is_song_info_command_enabled_by_default", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("is_song_info_command_enabled_by_default", value.toString())
            savePropertiesToFile()
        }

    var isEmptySongDisplayFilesOnPauseEnabledByDefault = getPropertyValue(
        properties, "is_empty_song_display_files_on_pause_enabled_by_default", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("is_empty_song_display_files_on_pause_enabled_by_default", value.toString())
            savePropertiesToFile()
        }

    var isAddSongCommandEnabledByDefault = getPropertyValue(
        properties, "is_add_song_command_enabled_by_default", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("is_add_song_command_enabled_by_default", value.toString())
            savePropertiesToFile()
        }

    var isSkipSongCommandEnabledByDefault = getPropertyValue(
        properties, "is_skip_song_command_enabled_by_default", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("is_skip_song_command_enabled_by_default", value.toString())
            savePropertiesToFile()
        }

    var isRemoveSongFromQueueCommandEnabledByDefault = getPropertyValue(
        properties, "is_remove_song_from_queue_command_enabled_by_default", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("is_remove_song_from_queue_command_enabled_by_default", value.toString())
            savePropertiesToFile()
        }

    var addSongCommandSecurityLevelOnStartUp = try {
        CustomCommandPermissions.valueOf(
            getPropertyValue(properties, "add_song_command_security_level_on_start_up", botConfigFile.path)
        )
    } catch (e: Exception) {
        displayEnumParsingErrorWindow(
            propertyName = "add_song_command_security_level_on_start_up",
            propertyFilePath = botConfigFile.path,
            exception = e,
            enumClassValues = CustomCommandPermissions.values().map { it.toString() }
        )
        exitProcess(-1)
    }
        set(value) {
            field = value
            properties.setProperty("add_song_command_security_level_on_start_up", value.toString())
            savePropertiesToFile()
        }

    var skipSongCommandSecurityLevelOnStartUp = try {
        CustomCommandPermissions.valueOf(
            getPropertyValue(properties, "skip_song_command_security_level_on_start_up", botConfigFile.path)
        )

    } catch (e: Exception) {
        displayEnumParsingErrorWindow(
            propertyName = "skip_song_command_security_level_on_start_up",
            propertyFilePath = botConfigFile.path,
            exception = e,
            enumClassValues = CustomCommandPermissions.values().map { it.toString() }
        )
        exitProcess(-1)
    }
        set(value) {
            field = value
            properties.setProperty("skip_song_command_security_level_on_start_up", value.toString())
            savePropertiesToFile()
        }

    var customGroupUserNamesAddSongCommand: List<String> = getPropertyValue(
        properties, "custom_group_user_names_add_song_command", botConfigFile.path
    ).lowercase(Locale.getDefault()).split(",")
        set(value) {
            field = value
            properties.setProperty(
                "custom_group_user_names_add_song_command",
                value.joinToPropertiesString(",").lowercase(Locale.getDefault())
            )
            savePropertiesToFile()
        }

    var customGroupUserNamesSkipSongCommand: List<String> = getPropertyValue(
        properties, "custom_group_user_names_skip_song_command", botConfigFile.path
    ).lowercase(Locale.getDefault()).split(",")
        set(value) {
            field = value
            properties.setProperty(
                "custom_group_user_names_skip_song_command",
                value.joinToPropertiesString(",").lowercase(Locale.getDefault())
            )
            savePropertiesToFile()
        }

    var removeSongFromQueueCommandSecurityLevelOnStartUp = try {
        CustomCommandPermissions.valueOf(
            getPropertyValue(
                properties,
                "remove_song_from_queue_command_security_level_on_start_up",
                botConfigFile.path
            )
        )
    } catch (e: Exception) {
        displayEnumParsingErrorWindow(
            propertyName = "remove_song_from_queue_command_security_level_on_start_up",
            propertyFilePath = botConfigFile.path,
            exception = e,
            enumClassValues = CustomCommandPermissions.values().map { it.toString() }
        )
        exitProcess(-1)
    }
        set(value) {
            field = value
            properties.setProperty("remove_song_from_queue_command_security_level_on_start_up", value.toString())
            savePropertiesToFile()
        }

    var customGroupUserNamesRemoveSongFromQueueCommand: List<String> = getPropertyValue(
        properties, "custom_group_user_names_remove_song_from_queue_command", botConfigFile.path
    ).lowercase(Locale.getDefault()).split(",")
        set(value) {
            field = value
            properties.setProperty(
                "custom_group_user_names_remove_song_from_queue_command",
                value.joinToPropertiesString(",").lowercase(Locale.getDefault())
            )
            savePropertiesToFile()
        }

    private fun savePropertiesToFile() {
        properties.store(FileOutputStream(botConfigFile.path), null)
    }
}