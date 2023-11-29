package scripts

import java.io.File

// Compile with: kotlinc UpdateProperties.kt -include-runtime -d UpdateProperties_1-2-0.jar

const val latestVersion = "1.2.0"

val propertiesFilesToProperties = listOf(
    File("data\\properties\\twitchBotConfig.properties") to
    mapOf(
        // Since Version: 1.0.0
        Pair("channel", "channelName"),
        Pair("song_request_redeem_id", "UseTheNameIfYouDontHaveTheId"),
        // Since Version: 1.0.1
        Pair("song_request_emotes", "carJAM,catJAM,Jammies,blobDance"),
        // Since Version: 1.2.0
        Pair("is_spotify_song_name_getter_enabled_by_default", "true")
    ),
    File("data\\properties\\spotifyConfig.properties") to
    mapOf(
        // Since Version: 1.2.0
        Pair("spotify_client_id", "ABC123VeryLong")
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
                    outputString += "Added property: \"$property\" with default value \"$defaultValue\""
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