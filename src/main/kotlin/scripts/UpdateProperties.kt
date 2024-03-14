package scripts

import java.io.File

// Compile with: kotlinc UpdateProperties.kt -include-runtime -d UpdateProperties_2-0-0.jar

const val latestVersion = "2.0.0"

// In Version 2.0.0 almost all properties got renamed to camel case spelling. That's why all of them are noted down as
// "Since Version: 2.0.0" though their functionality exists for much longer already.
val propertiesFilesToProperties = listOf(
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
        Pair("songRequestEmotes", "carJAM,catJAM,Jammies,blobDance")
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
        Pair("customGroupUserNamesRemoveSongFromQueueCommand", "")
    )
)

// This file holds all properties, that should exist for the latest version in all files.
// Executing it will write the properties with default values of the latest version.
fun main() {
    try {

        val outputString = mutableListOf<String>()

        outputString += "Checking for updates, latest verson: $latestVersion"

        propertiesFilesToProperties.forEach { (file, properties) ->
            if (!file.exists()) {
                file.createNewFile()
                outputString += "Created properties file ${file.name}"
            }
            val propertyFileContent = file.readLines().toMutableList()

            properties.forEach { (property, defaultValue) ->
                if (propertyFileContent.find { it.contains(property) } == null) {
                    propertyFileContent += "$property=$defaultValue"
                    outputString += "Added property: \"$property\" to ${file.name} with default value \"$defaultValue\""
                }
            }

            file.writeText(propertyFileContent.joinToString("\n"))
        }

        outputString += "Successfully updated properties!"
        Runtime.getRuntime().exec(
            arrayOf(
                "cmd", "/c", "start", "cmd", "/k",
                "echo ${outputString.joinToString("& echo.")}"
            )
        )
    } catch (e: Exception) {
        Runtime.getRuntime().exec(
            arrayOf(
                "cmd", "/c", "start", "cmd", "/k",
                "echo An error occured, see the exception here:& echo.${e.message}"
            )
        )
    }
}