package scripts.archeived

import java.io.File

// Compile with: kotlinc Migrate_to_2-0-0.kt -include-runtime -d Migrate_to_2-0-0.jar

private const val newLine = "& echo."

private val filesToCreate = listOf(
    File("data\\properties\\botConfig.properties")
)

private val propertiesToMove = mapOf(
    listOf(
        "is_song_request_command_enabled_by_default",
        "blacklisted_users",
        "is_spotify_song_name_getter_enabled_by_default",
        "is_empty_song_display_files_on_pause_enabled_by_default",
        "is_skip_song_command_enabled_by_default",
        "is_remove_song_from_queue_command_enabled_by_default",
        "is_add_song_command_enabled_by_default",
        "is_song_request_enabled_by_default",
        "is_song_info_command_enabled_by_default",
        "show_new_version_available_window_on_start_up"
    ) to
    listOf(
        File("data\\properties\\twitchBotConfig.properties"),
        File("data\\properties\\botConfig.properties")
    ),
    listOf(
        "custom_group_user_names_remove_song_from_queue_command",
        "custom_group_user_names_skip_song_command",
        "custom_group_user_names_add_song_command",
        "remove_song_from_queue_command_security_level_on_start_up",
        "add_song_command_security_level_on_start_up",
        "skip_song_command_security_level_on_start_up"
    ) to
    listOf(
        File("data\\properties\\spotifyConfig.properties"),
        File("data\\properties\\botConfig.properties")
    )
)

val propertiesToRename = mapOf(
    File("data\\properties\\spotifyConfig.properties") to
    mapOf(
        "blocked_song_links" to "blockedSongLinks",
        "playlist_id_for_add_song_command" to "playlistIdForAddSongCommand",
        "spotify_client_id" to "spotifyClientId",
        "blocked_song_artists" to "blockedSongArtists",
        "maximum_length_minutes_song_request" to "maximumLengthSongRequestMinutes"
    ),
    File("data\\properties\\twitchBotConfig.properties") to
    mapOf(
        "default_user_cool_down" to "defaultUserCoolDownSeconds",
        "song_request_redeem_id" to "songRequestRedeemId",
        "command_prefix" to "commandPrefix",
        "default_command_cool_down" to "defaultCommandCoolDownSeconds",
        "song_request_emotes" to "songRequestEmotes",
        "blacklist_emote" to "blacklistMessage"
    ),
    File("data\\properties\\botConfig.properties") to
    mapOf(
        "is_song_request_command_enabled_by_default" to "isSongRequestCommandEnabled",
        "blacklisted_users" to "blacklistedUsers",
        "is_spotify_song_name_getter_enabled_by_default" to "isSpotifySongNameGetterEnabled",
        "is_empty_song_display_files_on_pause_enabled_by_default" to "isEmptySongDisplayFilesOnPauseEnabled",
        "is_skip_song_command_enabled_by_default" to "isSkipSongCommandEnabled",
        "is_remove_song_from_queue_command_enabled_by_default" to "isRemoveSongFromQueueCommandEnabled",
        "is_add_song_command_enabled_by_default" to "isAddSongCommandEnabled",
        "is_song_request_enabled_by_default" to "isSongRequestEnabled",
        "is_song_info_command_enabled_by_default" to "isSongInfoCommandEnabled",
        "show_new_version_available_window_on_start_up" to "isNewVersionCheckEnabled",
        "custom_group_user_names_remove_song_from_queue_command" to "customGroupUserNamesRemoveSongFromQueueCommand",
        "custom_group_user_names_skip_song_command" to "customGroupUserNamesSkipSongCommand",
        "custom_group_user_names_add_song_command" to "customGroupUserNamesAddSongCommand",
        "remove_song_from_queue_command_security_level_on_start_up" to "removeSongFromQueueCommandSecurityLevel",
        "add_song_command_security_level_on_start_up" to "addSongCommandSecurityLevel",
        "skip_song_command_security_level_on_start_up" to "skipSongCommandSecurityLevel"
    )
)

fun main(){
    try {
        val outputString = mutableListOf<String>()
        var migrated = false

        outputString += "Starting migration to version 2.0.0$newLine"


        filesToCreate.forEach {
            outputString += if(!it.exists()) {
                it.createNewFile()
                migrated = true
                "Created file \"${it.name}\""
            } else {
                "Skipped creation of file \"${it.name}\" since it seems to already have happened"
            }
        }

        propertiesToMove.forEach{ (properties, files) ->
            val sourceFile = files[0]
            val destinationFile = files[1]
            var sourceFileLines = sourceFile.readLines()

            outputString += newLine

            properties.forEach { property ->
                outputString += if (sourceFileLines.any { it.startsWith(property) }) {
                    val propertyLine = sourceFileLines.first{it.startsWith(property)}
                    val destinationFileLines = destinationFile.readLines()

                    destinationFile.writeText((destinationFileLines + propertyLine).joinToString("\n"))
                    sourceFileLines = sourceFileLines.filter { !it.startsWith(property) }
                    sourceFile.writeText(sourceFileLines.joinToString("\n"))

                    migrated = true
                    "Moved property \"$property\" from file \"$sourceFile\" to file \"$destinationFile\""
                } else {
                    "Skipped moving property \"$property\" because it does not exist in source file \"$sourceFile\""
                }
            }

            outputString += newLine
        }


        propertiesToRename.forEach { (file, properties) ->
            val configLines = file.readLines().toMutableList()

            outputString += newLine

            properties.forEach{ (oldPropertyName, newPropertyName) ->
                outputString += if(configLines.any { it.contains(oldPropertyName) }) {
                    val currentLine = configLines.first { it.contains(oldPropertyName) }
                    configLines -= currentLine
                    configLines += currentLine.replace(oldPropertyName, newPropertyName)
                    file.writeText(configLines.joinToString("\n"))

                    migrated = true
                    "Renamed property \"$oldPropertyName\" in file \"$file\" to new property name \"$newPropertyName\""
                } else {
                    "Skipped renaming property \"$oldPropertyName\" because it does not exist in file \"$file\""
                }
            }

            outputString += newLine
        }

        val twitchBotConfigFile = File("data\\properties\\twitchBotConfig.properties")
        val twitchBotConfigLines = twitchBotConfigFile.readLines().toMutableList()
        val lineContent = twitchBotConfigLines.first { it.startsWith("blacklistMessage") }

        outputString += if(lineContent.substringAfter("=").length <= 12 ) {
            twitchBotConfigLines.remove(lineContent)
            twitchBotConfigLines.add("blacklistMessage=Imagine not being a blacklisted user. Couldn't be you FeelsOkayMan")
            twitchBotConfigFile.writeText(twitchBotConfigLines.joinToString("\n"))
            migrated = true
            "Changed content of blacklistMessage in file \"$twitchBotConfigFile\" to full message"
        } else {
            "Skipped changing content of blacklistMessage in file \"$twitchBotConfigFile\""
        }

        outputString += newLine

        outputString += if(migrated) {
            "Migration completed."
        } else {
            "Nothing had to be migrated."
        }

        Runtime.getRuntime().exec(
            arrayOf(
                "cmd", "/c", "start", "cmd", "/k",
                "echo ${outputString.joinToString(newLine)}"
            )
        )
    } catch (e: Exception) {
        Runtime.getRuntime().exec(
            arrayOf(
                "cmd", "/c", "start", "cmd", "/k",
                "echo An error occured, see the exception here:$newLine${e.message}"
            )
        )
        e.printStackTrace()
    }
}