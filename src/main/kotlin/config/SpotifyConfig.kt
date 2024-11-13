package config
import addQuotationMarks
import getPropertyValue
import joinToLowercasePropertiesString
import joinToPropertiesString
import logger
import showErrorMessageWindow
import toDoublePropertiesString
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

object SpotifyConfig {
    private val spotifyConfigFile = File("data\\properties\\spotifyConfig.properties")
    private val properties = Properties().apply {
        if(!spotifyConfigFile.exists()) {
            logger.error(
                "Error while reading property file ${spotifyConfigFile.path} in SpotifyConfig init: " +
                "File does not exist!"
            )
            showErrorMessageWindow(
                title = "Error while reading properties file",
                message = "Property file ${spotifyConfigFile.path.addQuotationMarks()} does not exist!"
            )

            exitProcess(-1)
        }
        load(spotifyConfigFile.inputStream())
    }

    val spotifyClientSecret: String = File("data\\tokens\\spotifyClientSecret.txt").readText()
    val spotifyClientId: String = getPropertyValue(properties, "spotifyClientId", spotifyConfigFile.path)

    var playlistNameForAddSongCommand = ""

    var playlistIdForAddSongCommand: String = getPropertyValue(
        properties, "playlistIdForAddSongCommand", spotifyConfigFile.path
    )
        set(value) {
            field = value
            properties.setProperty("playlistIdForAddSongCommand", value)
            savePropertiesToFile()
        }


    var maximumLengthSongRequestMinutes: Duration = try {
        getPropertyValue(properties, "maximumLengthSongRequestMinutes", spotifyConfigFile.path)
            .toDouble().minutes
    } catch (e: NumberFormatException) {
        val defaultValue = 60.0.minutes
        logger.warn(
            "Invalid number found while parsing property maximumLengthSongRequestMinutes, setting to $defaultValue"
        )
        defaultValue
    }
        set(value) {
            field = value
            properties.setProperty(
                "maximumLengthSongRequestMinutes", value.toDoublePropertiesString(DurationUnit.MINUTES)
            )
            savePropertiesToFile()
        }

    var blockedSongLinks: List<String> = getPropertyValue(
        properties, "blockedSongLinks", spotifyConfigFile.path
    ).split(",").filter { it.isNotEmpty() }
        set(value) {
            field = value
            properties.setProperty("blockedSongLinks", value.joinToPropertiesString(","))
            savePropertiesToFile()
        }

    var blockedSongArtists: List<String> = getPropertyValue(
        properties, "blockedSongArtists", spotifyConfigFile.path
    ).lowercase(Locale.getDefault()).split(",").filter { it.isNotEmpty() }
        set(value) {
            field = value
            properties.setProperty("blockedSongArtists", value.joinToLowercasePropertiesString(","))
            savePropertiesToFile()
        }

    private fun savePropertiesToFile() {
        try {
            properties.store(FileOutputStream(spotifyConfigFile.path), null)
        } catch (e: Exception) {
            logger.error("Error while saving properties to file in SpotifyConfig.")
            logger.error(e.stackTraceToString())
        }
    }
}