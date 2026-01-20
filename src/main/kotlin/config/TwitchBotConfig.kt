package config

import addQuotationMarks
import getPropertyValue
import joinToPropertiesString
import logger
import redeems.songRequestRedeem
import showErrorMessageWindow
import toDoublePropertiesString
import toIntPropertiesString
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
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
                message = "Property file ${twitchBotConfigFile.path.addQuotationMarks()} does not exist!"
            )

            exitProcess(-1)
        }
        load(twitchBotConfigFile.inputStream())
    }

    val chatAccountToken = File("data\\tokens\\twitchToken.txt").readText()
    // Will be filled with the ID in function setupTwitchBot
    var chatAccountID = ""

    val channel: String = getPropertyValue(
        properties,
        propertyName = "channel",
        propertiesFileRelativePath = twitchBotConfigFile.path,
        setPropertyIfNotExisting = false
    ).lowercase()

    var commandPrefix: String = getPropertyValue(
        properties,
        propertyName = "commandPrefix",
        propertiesFileRelativePath = twitchBotConfigFile.path,
        setPropertyIfNotExisting = false
    )
        set(value) {
            field = value
            properties.setProperty("commandPrefix", value)
            savePropertiesToFile()
        }

    var defaultCommandCoolDownSeconds = try {
        getPropertyValue(
            properties,
            propertyName = "defaultCommandCoolDownSeconds",
            propertiesFileRelativePath = twitchBotConfigFile.path,
            setPropertyIfNotExisting = false
        ).toInt().seconds
    } catch (_: NumberFormatException) {
        val defaultValue = 0.seconds
        logger.warn(
            "Invalid number found while parsing property defaultCommandCoolDownSeconds, setting to $defaultValue"
        )
        defaultValue
    }
        set(value) {
            field = value
            properties.setProperty("defaultCommandCoolDownSeconds", value.toIntPropertiesString(DurationUnit.SECONDS))
            savePropertiesToFile()
        }

    var defaultUserCoolDownSeconds = try {
        getPropertyValue(
            properties,
            propertyName = "defaultUserCoolDownSeconds",
            propertiesFileRelativePath = twitchBotConfigFile.path,
            setPropertyIfNotExisting = false
        ).toInt().seconds
    } catch (_: NumberFormatException) {
        val defaultValue = 0.seconds
        logger.warn(
            "Invalid number found while parsing property defaultUserCoolDownSeconds, setting to $defaultValue"
        )
        defaultValue
    }
        set(value) {
            field = value
            properties.setProperty("defaultUserCoolDownSeconds", value.toIntPropertiesString(DurationUnit.SECONDS))
            savePropertiesToFile()
        }

    var songRequestRedeemId: String = getPropertyValue(
        properties,
        propertyName = "songRequestRedeemId",
        propertiesFileRelativePath = twitchBotConfigFile.path,
        setPropertyIfNotExisting = false
    )
        set(value) {
            field = value
            properties.setProperty("songRequestRedeemId", value)
            songRequestRedeem.id = value
            savePropertiesToFile()
        }

    var songRequestEmotes: List<String> = getPropertyValue(
        properties,
        propertyName = "songRequestEmotes",
        propertiesFileRelativePath = twitchBotConfigFile.path,
        setPropertyIfNotExisting = false
    ).split(",").filter { it.isNotEmpty() }
        set(value) {
            field = value
            properties.setProperty("songRequestEmotes", value.joinToPropertiesString(","))
            savePropertiesToFile()
        }

    var blacklistMessage: String = getPropertyValue(
        properties,
        propertyName = "blacklistMessage",
        propertiesFileRelativePath = twitchBotConfigFile.path,
        setPropertyIfNotExisting = false
    )
        set(value) {
            field = value
            properties.setProperty("blacklistMessage", value)
            savePropertiesToFile()
        }


    var minimumFollowingDurationMinutes: Duration = try {
        getPropertyValue(
            properties,
            propertyName = "minimumFollowingDurationMinutes",
            propertiesFileRelativePath = twitchBotConfigFile.path,
            setPropertyIfNotExisting = false
        ).toDouble().minutes
    } catch (_: NumberFormatException) {
        val defaultValue = 0.minutes
        logger.warn(
            "Invalid number found while parsing property minimumFollowingDurationMinutes, setting to $defaultValue"
        )
        defaultValue
    }
        set(value) {
            field = value
            properties.setProperty(
                "minimumFollowingDurationMinutes", value.toDoublePropertiesString(DurationUnit.MINUTES)
            )
            savePropertiesToFile()
        }


    private fun savePropertiesToFile() {
        try {
            properties.store(FileOutputStream(twitchBotConfigFile.path), null)
        } catch (e: Exception) {
            logger.error("Error while saving properties to file in BotConfig.")
            logger.error(e.stackTraceToString())
        }
    }
}