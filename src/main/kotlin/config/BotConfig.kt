package config

import CustomCommandPermissions
import displayEnumParsingErrorWindow
import getPropertyValue
import joinToLowercasePropertiesString
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


    var isSongRequestCommandEnabled = getPropertyValue(
        properties, "isSongRequestCommandEnabled", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isSongRequestCommandEnabled", value.toString())
            savePropertiesToFile()
        }

    var blacklistedUsers = getPropertyValue(
        properties, "blacklistedUsers", botConfigFile.path
    ).lowercase(Locale.getDefault()).split(",").filter { it.isNotEmpty() }
        set(value) {
            field = value
            properties.setProperty("blacklistedUsers", value.joinToLowercasePropertiesString(","))
            savePropertiesToFile()
        }

    var isSpotifySongNameGetterEnabled = getPropertyValue(
        properties, "isSpotifySongNameGetterEnabled", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isSpotifySongNameGetterEnabled", value.toString())
            savePropertiesToFile()
        }

    var isNewVersionCheckEnabled = getPropertyValue(
        properties, "isNewVersionCheckEnabled", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isNewVersionCheckEnabled", value.toString())
            savePropertiesToFile()
        }

    var isSongRequestEnabled = getPropertyValue(
        properties, "isSongRequestEnabled", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isSongRequestEnabled", value.toString())
            savePropertiesToFile()
        }

    var isSongInfoCommandEnabled = getPropertyValue(
        properties, "isSongInfoCommandEnabled", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isSongInfoCommandEnabled", value.toString())
            savePropertiesToFile()
        }

    var isEmptySongDisplayFilesOnPauseEnabled = getPropertyValue(
        properties, "isEmptySongDisplayFilesOnPauseEnabled", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isEmptySongDisplayFilesOnPauseEnabled", value.toString())
            savePropertiesToFile()
        }

    var isAddSongCommandEnabled = getPropertyValue(
        properties, "isAddSongCommandEnabled", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isAddSongCommandEnabled", value.toString())
            savePropertiesToFile()
        }

    var isSkipSongCommandEnabled = getPropertyValue(
        properties, "isSkipSongCommandEnabled", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isSkipSongCommandEnabled", value.toString())
            savePropertiesToFile()
        }

    var isRemoveSongFromQueueCommandEnabled = getPropertyValue(
        properties, "isRemoveSongFromQueueCommandEnabled", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isRemoveSongFromQueueCommandEnabled", value.toString())
            savePropertiesToFile()
        }

    var addSongCommandSecurityLevel = try {
        CustomCommandPermissions.valueOf(
            getPropertyValue(properties, "addSongCommandSecurityLevel", botConfigFile.path)
        )
    } catch (e: Exception) {
        displayEnumParsingErrorWindow(
            propertyName = "addSongCommandSecurityLevel",
            propertyFilePath = botConfigFile.path,
            exception = e,
            enumClassValues = CustomCommandPermissions.values().map { it.toString() }
        )
        exitProcess(-1)
    }
        set(value) {
            field = value
            properties.setProperty("addSongCommandSecurityLevel", value.toString())
            savePropertiesToFile()
        }

    var skipSongCommandSecurityLevel = try {
        CustomCommandPermissions.valueOf(
            getPropertyValue(properties, "skipSongCommandSecurityLevel", botConfigFile.path)
        )

    } catch (e: Exception) {
        displayEnumParsingErrorWindow(
            propertyName = "skipSongCommandSecurityLevel",
            propertyFilePath = botConfigFile.path,
            exception = e,
            enumClassValues = CustomCommandPermissions.values().map { it.toString() }
        )
        exitProcess(-1)
    }
        set(value) {
            field = value
            properties.setProperty("skipSongCommandSecurityLevel", value.toString())
            savePropertiesToFile()
        }

    var customGroupUserNamesAddSongCommand: List<String> = getPropertyValue(
        properties, "customGroupUserNamesAddSongCommand", botConfigFile.path
    ).lowercase(Locale.getDefault()).split(",").filter { it.isNotEmpty() }
        set(value) {
            field = value
            properties.setProperty(
                "customGroupUserNamesAddSongCommand", value.joinToLowercasePropertiesString(",")
            )
            savePropertiesToFile()
        }

    var customGroupUserNamesSkipSongCommand: List<String> = getPropertyValue(
        properties, "customGroupUserNamesSkipSongCommand", botConfigFile.path
    ).lowercase(Locale.getDefault()).split(",").filter { it.isNotEmpty() }
        set(value) {
            field = value
            properties.setProperty(
                "customGroupUserNamesSkipSongCommand", value.joinToLowercasePropertiesString(",")
            )
            savePropertiesToFile()
        }

    var removeSongFromQueueCommandSecurityLevel = try {
        CustomCommandPermissions.valueOf(
            getPropertyValue(
                properties,
                "removeSongFromQueueCommandSecurityLevel",
                botConfigFile.path
            )
        )
    } catch (e: Exception) {
        displayEnumParsingErrorWindow(
            propertyName = "removeSongFromQueueCommandSecurityLevel",
            propertyFilePath = botConfigFile.path,
            exception = e,
            enumClassValues = CustomCommandPermissions.values().map { it.toString() }
        )
        exitProcess(-1)
    }
        set(value) {
            field = value
            properties.setProperty("removeSongFromQueueCommandSecurityLevel", value.toString())
            savePropertiesToFile()
        }

    var customGroupUserNamesRemoveSongFromQueueCommand: List<String> = getPropertyValue(
        properties, "customGroupUserNamesRemoveSongFromQueueCommand", botConfigFile.path
    ).lowercase(Locale.getDefault()).split(",").filter { it.isNotEmpty() }
        set(value) {
            field = value
            properties.setProperty(
                "customGroupUserNamesRemoveSongFromQueueCommand", value.joinToLowercasePropertiesString(",")
            )
            savePropertiesToFile()
        }

    var isBlockSongCommandEnabled = getPropertyValue(
        properties, "isBlockSongCommandEnabled", botConfigFile.path
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isBlockSongCommandEnabled", value.toString())
            savePropertiesToFile()
        }

    var blockSongCommandSecurityLevel = try {
        CustomCommandPermissions.valueOf(
            getPropertyValue(
                properties,
                "blockSongCommandSecurityLevel",
                botConfigFile.path
            )
        )
    } catch (e: Exception) {
        displayEnumParsingErrorWindow(
            propertyName = "blockSongCommandSecurityLevel",
            propertyFilePath = botConfigFile.path,
            exception = e,
            enumClassValues = CustomCommandPermissions.values().map { it.toString() }
        )
        exitProcess(-1)
    }
        set(value) {
            field = value
            properties.setProperty("blockSongCommandSecurityLevel", value.toString())
            savePropertiesToFile()
        }

    var customGroupUserNamesBlockSongCommand: List<String> = getPropertyValue(
        properties, "customGroupUserNamesBlockSongCommand", botConfigFile.path
    ).lowercase(Locale.getDefault()).split(",").filter { it.isNotEmpty() }
        set(value) {
            field = value
            properties.setProperty(
                "customGroupUserNamesBlockSongCommand", value.joinToLowercasePropertiesString(",")
            )
            savePropertiesToFile()
        }

    private fun savePropertiesToFile() {
        properties.store(FileOutputStream(botConfigFile.path), null)
    }
}