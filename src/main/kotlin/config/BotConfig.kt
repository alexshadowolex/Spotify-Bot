package config

import CustomCommandPermissions
import addQuotationMarks
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
                message = "Property file ${botConfigFile.path.addQuotationMarks()} does not exist!"
            )

            exitProcess(-1)
        }
        load(botConfigFile.inputStream())
    }


    var isSongRequestCommandEnabled = getPropertyValue(
        properties,
        propertyName = "isSongRequestCommandEnabled",
        propertiesFileRelativePath = botConfigFile.path,
        setPropertyIfNotExisting = false
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isSongRequestCommandEnabled", value.toString())
            savePropertiesToFile()
        }

    var blacklistedUsers = getPropertyValue(
        properties,
        propertyName = "blacklistedUsers",
        propertiesFileRelativePath = botConfigFile.path,
        setPropertyIfNotExisting = false
    ).lowercase(Locale.getDefault()).split(",").filter { it.isNotEmpty() }
        set(value) {
            field = value
            properties.setProperty("blacklistedUsers", value.joinToLowercasePropertiesString(","))
            savePropertiesToFile()
        }

    var isSpotifySongNameGetterEnabled = getPropertyValue(
        properties,
        propertyName = "isSpotifySongNameGetterEnabled",
        propertiesFileRelativePath = botConfigFile.path,
        setPropertyIfNotExisting = false
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isSpotifySongNameGetterEnabled", value.toString())
            savePropertiesToFile()
        }

    var isNewVersionCheckEnabled = getPropertyValue(
        properties,
        propertyName = "isNewVersionCheckEnabled",
        propertiesFileRelativePath = botConfigFile.path,
        setPropertyIfNotExisting = false
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isNewVersionCheckEnabled", value.toString())
            savePropertiesToFile()
        }

    var isSongRequestEnabled = getPropertyValue(
        properties,
        propertyName = "isSongRequestEnabled",
        propertiesFileRelativePath = botConfigFile.path,
        setPropertyIfNotExisting = false
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isSongRequestEnabled", value.toString())
            savePropertiesToFile()
        }

    var isSongInfoCommandEnabled = getPropertyValue(
        properties,
        propertyName = "isSongInfoCommandEnabled",
        propertiesFileRelativePath = botConfigFile.path,
        setPropertyIfNotExisting = false
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isSongInfoCommandEnabled", value.toString())
            savePropertiesToFile()
        }

    var isEmptySongDisplayFilesOnPauseEnabled = getPropertyValue(
        properties,
        propertyName = "isEmptySongDisplayFilesOnPauseEnabled",
        propertiesFileRelativePath = botConfigFile.path,
        setPropertyIfNotExisting = false
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isEmptySongDisplayFilesOnPauseEnabled", value.toString())
            savePropertiesToFile()
        }

    var isAddSongCommandEnabled = getPropertyValue(
        properties,
        propertyName = "isAddSongCommandEnabled",
        propertiesFileRelativePath = botConfigFile.path,
        setPropertyIfNotExisting = false
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isAddSongCommandEnabled", value.toString())
            savePropertiesToFile()
        }

    var isSkipSongCommandEnabled = getPropertyValue(
        properties,
        propertyName = "isSkipSongCommandEnabled",
        propertiesFileRelativePath = botConfigFile.path,
        setPropertyIfNotExisting = false
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isSkipSongCommandEnabled", value.toString())
            savePropertiesToFile()
        }

    var isRemoveSongFromQueueCommandEnabled = getPropertyValue(
        properties,
        propertyName = "isRemoveSongFromQueueCommandEnabled",
        propertiesFileRelativePath = botConfigFile.path,
        setPropertyIfNotExisting = false
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isRemoveSongFromQueueCommandEnabled", value.toString())
            savePropertiesToFile()
        }

    var addSongCommandSecurityLevel = try {
        CustomCommandPermissions.valueOf(
            getPropertyValue(
                properties,
                propertyName = "addSongCommandSecurityLevel",
                propertiesFileRelativePath = botConfigFile.path,
                setPropertyIfNotExisting = false
            )
        )
    } catch (e: Exception) {
        displayEnumParsingErrorWindow(
            propertyName = "addSongCommandSecurityLevel",
            propertyFilePath = botConfigFile.path,
            exception = e,
            enumClassValues = CustomCommandPermissions.entries.map { it.toString() }
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
            getPropertyValue(
                properties,
                propertyName = "skipSongCommandSecurityLevel",
                propertiesFileRelativePath = botConfigFile.path,
                setPropertyIfNotExisting = false
            )
        )

    } catch (e: Exception) {
        displayEnumParsingErrorWindow(
            propertyName = "skipSongCommandSecurityLevel",
            propertyFilePath = botConfigFile.path,
            exception = e,
            enumClassValues = CustomCommandPermissions.entries.map { it.toString() }
        )
        exitProcess(-1)
    }
        set(value) {
            field = value
            properties.setProperty("skipSongCommandSecurityLevel", value.toString())
            savePropertiesToFile()
        }

    var customGroupUserNamesAddSongCommand: List<String> = getPropertyValue(
        properties,
        propertyName = "customGroupUserNamesAddSongCommand",
        propertiesFileRelativePath = botConfigFile.path,
        setPropertyIfNotExisting = false
    ).lowercase(Locale.getDefault()).split(",").filter { it.isNotEmpty() }
        set(value) {
            field = value
            properties.setProperty(
                "customGroupUserNamesAddSongCommand", value.joinToLowercasePropertiesString(",")
            )
            savePropertiesToFile()
        }

    var customGroupUserNamesSkipSongCommand: List<String> = getPropertyValue(
        properties,
        propertyName = "customGroupUserNamesSkipSongCommand",
        propertiesFileRelativePath = botConfigFile.path,
        setPropertyIfNotExisting = false
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
                propertyName = "removeSongFromQueueCommandSecurityLevel",
                propertiesFileRelativePath = botConfigFile.path,
                setPropertyIfNotExisting = false
            )
        )
    } catch (e: Exception) {
        displayEnumParsingErrorWindow(
            propertyName = "removeSongFromQueueCommandSecurityLevel",
            propertyFilePath = botConfigFile.path,
            exception = e,
            enumClassValues = CustomCommandPermissions.entries.map { it.toString() }
        )
        exitProcess(-1)
    }
        set(value) {
            field = value
            properties.setProperty("removeSongFromQueueCommandSecurityLevel", value.toString())
            savePropertiesToFile()
        }

    var customGroupUserNamesRemoveSongFromQueueCommand: List<String> = getPropertyValue(
        properties,
        propertyName = "customGroupUserNamesRemoveSongFromQueueCommand",
        propertiesFileRelativePath = botConfigFile.path,
        setPropertyIfNotExisting = false
    ).lowercase(Locale.getDefault()).split(",").filter { it.isNotEmpty() }
        set(value) {
            field = value
            properties.setProperty(
                "customGroupUserNamesRemoveSongFromQueueCommand", value.joinToLowercasePropertiesString(",")
            )
            savePropertiesToFile()
        }

    var isBlockSongCommandEnabled = getPropertyValue(
        properties,
        propertyName = "isBlockSongCommandEnabled",
        propertiesFileRelativePath = botConfigFile.path,
        setPropertyIfNotExisting = false
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
                propertyName = "blockSongCommandSecurityLevel",
                propertiesFileRelativePath = botConfigFile.path,
                setPropertyIfNotExisting = false
            )
        )
    } catch (e: Exception) {
        displayEnumParsingErrorWindow(
            propertyName = "blockSongCommandSecurityLevel",
            propertyFilePath = botConfigFile.path,
            exception = e,
            enumClassValues = CustomCommandPermissions.entries.map { it.toString() }
        )
        exitProcess(-1)
    }
        set(value) {
            field = value
            properties.setProperty("blockSongCommandSecurityLevel", value.toString())
            savePropertiesToFile()
        }

    var customGroupUserNamesBlockSongCommand: List<String> = getPropertyValue(
        properties,
        propertyName = "customGroupUserNamesBlockSongCommand",
        propertiesFileRelativePath = botConfigFile.path,
        setPropertyIfNotExisting = false
    ).lowercase(Locale.getDefault()).split(",").filter { it.isNotEmpty() }
        set(value) {
            field = value
            properties.setProperty(
                "customGroupUserNamesBlockSongCommand", value.joinToLowercasePropertiesString(",")
            )
            savePropertiesToFile()
        }

    var isFollowerOnlyModeEnabled = getPropertyValue(
        properties,
        propertyName = "isFollowerOnlyModeEnabled",
        propertiesFileRelativePath = botConfigFile.path,
        setPropertyIfNotExisting = false
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isFollowerOnlyModeEnabled", value.toString())
            savePropertiesToFile()
        }

    var isPauseResumeCommandEnabled = getPropertyValue(
        properties,
        propertyName = "isPauseResumeCommandEnabled",
        propertiesFileRelativePath = botConfigFile.path,
        setPropertyIfNotExisting = false
    ).toBoolean()
        set(value) {
            field = value
            properties.setProperty("isPauseResumeCommandEnabled", value.toString())
            savePropertiesToFile()
        }

    var pauseResumeCommandSecurityLevel = try {
        CustomCommandPermissions.valueOf(
            getPropertyValue(
                properties,
                propertyName = "pauseResumeCommandSecurityLevel",
                propertiesFileRelativePath = botConfigFile.path,
                setPropertyIfNotExisting = false
            )
        )
    } catch (e: Exception) {
        displayEnumParsingErrorWindow(
            propertyName = "pauseResumeCommandSecurityLevel",
            propertyFilePath = botConfigFile.path,
            exception = e,
            enumClassValues = CustomCommandPermissions.entries.map { it.toString() }
        )
        exitProcess(-1)
    }

    var customGroupUserNamesPauseResumeCommand: List<String> = getPropertyValue(
        properties,
        propertyName = "customGroupUserNamesPauseResumeCommand",
        propertiesFileRelativePath = botConfigFile.path,
        setPropertyIfNotExisting = false
    ).lowercase(Locale.getDefault()).split(",").filter { it.isNotEmpty() }
        set(value) {
            field = value
            properties.setProperty(
                "customGroupUserNamesPauseResumeCommand", value.joinToLowercasePropertiesString(",")
            )
            savePropertiesToFile()
        }

    private fun savePropertiesToFile() {
        try {
            properties.store(FileOutputStream(botConfigFile.path), null)
        } catch (e: Exception) {
            logger.error("Error while saving properties to file in BotConfig.")
            logger.error(e.stackTraceToString())
        }
    }
}