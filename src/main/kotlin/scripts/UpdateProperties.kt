package scripts

import java.io.File

// Compile with: kotlinc UpdateProperties.kt -include-runtime -d UpdateProperties_1-2-4.jar

const val latestVersion = "1.2.4"

val propertiesFilesToProperties = listOf(
    File("data\\properties\\twitchBotConfig.properties") to
    mapOf(
        // Since Version: 1.0.0
        Pair("channel", "channelName"),
        Pair("command_prefix", "#"),
        Pair("default_command_cool_down", "20"),
        Pair("default_user_cool_down", "30"),
        Pair("song_request_redeem_id", "UseTheNameIfYouDontHaveTheId"),
        Pair("is_song_request_command_enabled_by_default", "true"),
        Pair("blacklisted_users", ""),
        Pair("blacklist_emote", "FeelsOkayMan"),
        // Since Version: 1.0.1
        Pair("song_request_emotes", "carJAM,catJAM,Jammies,blobDance"),
        // Since Version: 1.2.0
        Pair("is_spotify_song_name_getter_enabled_by_default", "true"),
        // Since Version: 1.2.1
        Pair("show_new_version_available_window_on_start_up", "true"),
        // Since Version: 1.2.3
        Pair("is_song_request_enabled_by_default", "true"),
        Pair("is_song_info_command_enabled_by_default", "true"),
        Pair("is_empty_song_display_files_on_pause_enabled_by_default", "true"),
        // Since Version: 1.2.4
        Pair("is_add_song_command_enabled_by_default", "true")
    ),
    File("data\\properties\\spotifyConfig.properties") to
    mapOf(
        // Since Version: 1.2.0
        Pair("spotify_client_id", "ABC123VeryLong"),
        // Since Version: 1.2.4
        Pair("playlist_id_for_add_song_command", "playlistId"),
        Pair("add_song_command_security_level_on_start_up", "BROADCASTER"),
        Pair("maximum_length_minutes_song_request", "")
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