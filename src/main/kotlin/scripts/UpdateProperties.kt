package scripts

import java.io.File

// Compile with: kotlinc UpdateProperties.kt -include-runtime -d UpdateProperties_1-0-1.jar

const val latestVersion = "1.0.1"

val defaultPropertiesValues = listOf(
    // TwitchBotConfig properties
    mapOf(
        // Since Version: 1.0.0
        Pair("channel", "channelName"),
        Pair("spotify_client_id", "ABC123VeryLong"),
        Pair("spotify_client_secret", "123ABCVeryLong"),
        Pair("song_request_redeem_id", "UseTheNameIfYouDontHaveTheId"),
        // Since Version: 1.0.1
        Pair("song_request_emotes", "carJAM,catJAM,Jammies,blobDance")
    )
)

// This file holds all properties, that should exist for the latest version in all files.
// Executing it will write the properties with default values of the latest version.
fun main() {
    try {
        val propertiesFiles = listOf(
            File("data\\twitchBotConfig.properties")
        )

        val outputString = mutableListOf<String>()

        outputString += "Checking for updates, latest verson: $latestVersion"

        defaultPropertiesValues.forEachIndexed { index, currentDefaultPropertiesMap ->
            val currentPropertiesFile = propertiesFiles[index]
            var currentContent = mutableListOf<String>()
            currentDefaultPropertiesMap.forEach { property ->
                if (!currentPropertiesFile.exists()) {
                    currentPropertiesFile.createNewFile()
                    outputString += "Created properties file ${currentPropertiesFile.name}"
                } else if (currentContent.isEmpty()) {
                    currentContent = currentPropertiesFile.readLines().toMutableList()
                }

                if (currentContent.find { it.contains(property.key) } == null) {
                    currentContent += (property.key + "=" + property.value)
                    outputString += "Added property: \"${property.key}\" with default value \"${property.value}\""
                }
            }

            currentPropertiesFile.writeText(currentContent.joinToString("\n"))
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