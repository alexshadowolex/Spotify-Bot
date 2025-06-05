package scripts

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

// Compile with: kotlinc UpdateProperties.kt -include-runtime -d UpdateProperties_2-0-3.jar

private const val latestVersion = "2.0.3"
private const val LOG_DIRECTORY = "logs\\update"

// In Version 2.0.0, almost all properties got renamed to camel case spelling. That's why all of them are noted down as
// "Since Version: 2.0.0" though their functionality exists for much longer already.
private val propertiesFilesToProperties = listOf(
    File("data\\properties\\twitchBotConfig.properties") to
    mapOf(
        // Since Version: 1.0.0
        Pair("channel", "channelName"),
        // Since Version: 2.0.0
        Pair("commandPrefix", "#"),
        Pair("defaultCommandCoolDownSeconds", "20"),
        Pair("defaultUserCoolDownSeconds", "30"),
        Pair("songRequestRedeemId", "UseTheNameIfYouDontHaveTheId"),
        Pair("blacklistMessage", "Imagine not being a blacklisted user. Couldn't be you FeelsOkayMan"),
        Pair("songRequestEmotes", "carJAM,catJAM,Jammies,blobDance"),
        // Since Version: 2.0.3
        Pair("minimumFollowingDurationMinutes", "0")
    ),
    File("data\\properties\\spotifyConfig.properties") to
    mapOf(
        // Since Version: 2.0.0
        Pair("spotifyClientId", "ABC123VeryLong"),
        Pair("playlistIdForAddSongCommand", "playlistId"),
        Pair("maximumLengthSongRequestMinutes", ""),
        Pair("blockedSongLinks", ""),
        Pair("blockedSongArtists", "")
    ),
    File("data\\properties\\botConfig.properties") to
    mapOf(
        // Since Version: 2.0.0
        Pair("isSongRequestCommandEnabled", "true"),
        Pair("blacklistedUsers", ""),
        Pair("isSpotifySongNameGetterEnabled", "true"),
        Pair("isNewVersionCheckEnabled", "true"),
        Pair("isSongRequestEnabled", "true"),
        Pair("isSongInfoCommandEnabled", "true"),
        Pair("isEmptySongDisplayFilesOnPauseEnabled", "true"),
        Pair("isAddSongCommandEnabled", "true"),
        Pair("isSkipSongCommandEnabled", "true"),
        Pair("isRemoveSongFromQueueCommandEnabled", "true"),
        Pair("addSongCommandSecurityLevel", "BROADCASTER"),
        Pair("skipSongCommandSecurityLevel", "BROADCASTER"),
        Pair("customGroupUserNamesAddSongCommand", ""),
        Pair("customGroupUserNamesSkipSongCommand", ""),
        Pair("removeSongFromQueueCommandSecurityLevel", "BROADCASTER"),
        Pair("customGroupUserNamesRemoveSongFromQueueCommand", ""),
        // Since Version: 2.0.1
        Pair("isBlockSongCommandEnabled", "true"),
        Pair("blockSongCommandSecurityLevel", "BROADCASTER"),
        Pair("customGroupUserNamesBlockSongCommand", ""),
        // Since Version: 2.0.3
        Pair("isFollowerOnlyModeEnabled", "false")
    )
)

private val outputString = mutableListOf<String>()
private var isJarStartedByAutoUpdate = false

// This file holds all properties, that should exist for the latest version in all files.
// Executing it will write the properties with default values of the latest version.
fun main(args: Array<String>) {
    var didAnythingGetUpdated = false
    var isUpdateSuccessfull = true
    isJarStartedByAutoUpdate = args.isNotEmpty()

    try {
        logLine("=============================================")
        logLine("Checking for all non-existing properties with latest version: $latestVersion")

        propertiesFilesToProperties.forEach { (file, properties) ->
            if (!file.exists()) {
                file.createNewFile()
                logLine("Created properties file ${file.name}")
            }
            val propertyFileContent = file.readLines().toMutableList()

            properties.forEach { (property, defaultValue) ->
                if (propertyFileContent.find { it.contains(property) } == null) {
                    didAnythingGetUpdated = true
                    propertyFileContent += "$property=$defaultValue"
                    logLine("Added property: \"$property\" to ${file.name} with default value \"$defaultValue\"")
                }
            }

            file.writeText(propertyFileContent.joinToString("\n"))
        }

        val resultMessage = if(didAnythingGetUpdated) {
            "Successfully updated properties!"
        } else {
            "No properties-file had to be updated!"
        }
        logLine(resultMessage)
        logLine("=============================================")
    } catch (e: Exception) {
        logLine("An error occurred, see the exception here:& echo.${e.message}")
        logLine("=============================================")
        isUpdateSuccessfull = false
    }

    Files.createDirectories(Paths.get(LOG_DIRECTORY))

    val logFileName = DateTimeFormatter
        .ISO_INSTANT
        .format(java.time.Instant.now())
        .replace(':', '-')

    Paths.get(LOG_DIRECTORY, "UpdateProperties_${logFileName}.log").toFile().also {
        if (!it.exists()) {
            it.createNewFile()
        }
    }.writeText(outputString.joinToString("\n"))

    if(!isJarStartedByAutoUpdate) {
        ProcessBuilder(
            "cmd", "/c", "start", "cmd", "/k",
            "echo ${outputString.joinToString("& echo ")} && pause & exit"
        ).start()
    }

    val exitCode = if(isUpdateSuccessfull) {
        0
    } else {
        -1
    }

    exitProcess(exitCode)
}


/**
 * Logs the line to the output string, which will be displayed in the console and in the log-file at the end. If the
 * variable "isJarStartedByAutoUpdate" is true, it also immediately logs it to the console.
 * @param line Message-Line to be logged
 */
fun logLine(line: String) {
    if(isJarStartedByAutoUpdate) {
        println(line)
    }

    outputString += line
}