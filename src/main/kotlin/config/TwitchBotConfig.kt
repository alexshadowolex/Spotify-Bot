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

    var blacklistEmote: String = getPropertyValue(properties, "blacklist_emote", twitchBotConfigFile.path)
        set(value) {
            field = value
            properties.setProperty("blacklist_emote", value)
            savePropertiesToFile()
        }


    private fun savePropertiesToFile() {
        properties.store(FileOutputStream(twitchBotConfigFile.path), null)
    }
}