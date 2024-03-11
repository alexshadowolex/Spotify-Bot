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

    var commandPrefix: String = getPropertyValue(properties, "commandPrefix", twitchBotConfigFile.path)
        set(value) {
            field = value
            properties.setProperty("commandPrefix", value)
            savePropertiesToFile()
        }

    var defaultCommandCoolDown = getPropertyValue(
        properties, "defaultCommandCoolDown", twitchBotConfigFile.path
    ).toInt().seconds
        set(value) {
            field = value
            properties.setProperty("defaultCommandCoolDown", value.toInt(DurationUnit.SECONDS).toString())
            savePropertiesToFile()
        }

    var defaultUserCoolDown = getPropertyValue(
        properties, "defaultUserCoolDown", twitchBotConfigFile.path
    ).toInt().seconds
        set(value) {
            field = value
            properties.setProperty("defaultUserCoolDown", value.toInt(DurationUnit.SECONDS).toString())
            savePropertiesToFile()
        }

    val songRequestRedeemId: String = getPropertyValue(
        properties, "songRequestRedeemId", twitchBotConfigFile.path
    )

    var songRequestEmotes: List<String> = getPropertyValue(
        properties, "songRequestEmotes", twitchBotConfigFile.path
    ).split(",")
        set(value) {
            field = value
            properties.setProperty("songRequestEmotes", value.joinToPropertiesString(","))
            savePropertiesToFile()
        }

    var blacklistMessage: String = getPropertyValue(properties, "blacklistMessage", twitchBotConfigFile.path)
        set(value) {
            field = value
            properties.setProperty("blacklistMessage", value)
            savePropertiesToFile()
        }


    private fun savePropertiesToFile() {
        properties.store(FileOutputStream(twitchBotConfigFile.path), null)
    }
}