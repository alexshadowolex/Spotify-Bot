package scripts

import java.io.File

const val newLine = "& echo."
val renameFiles = mapOf(
    "twitchBotconfig.properties" to "twitchBotConfig.properties",
    "twitchtoken.txt" to "twitchToken.txt"
)

val filesToMoveToNewFolder = mapOf(
    File("data\\properties") to
    listOf(
        File("data\\twitchBotconfig.properties")
    ),
    File("data\\tokens") to
    listOf(
        File("data\\twitchtoken.txt"),
        File("data\\spotifyToken.json")
    )
)

val filesToCreate = listOf(
    File("data\\properties\\spotifyConfig.properties"),
    File("data\\tokens\\spotifyClientSecret.txt")
)

val propertiesToMove = mapOf(
    "spotify_client_id" to
    listOf(
        File("data\\properties\\twitchBotConfig.properties"),
        File("data\\properties\\spotifyConfig.properties")
    ),
    "spotify_client_secret" to
    listOf(
        File("data\\properties\\twitchBotConfig.properties"),
        File("data\\tokens\\spotifyClientSecret.txt")
    )
)

fun main(){
    try {
        val outputString = mutableListOf<String>()
        var migrated = false

        outputString += "Starting migration to version 1.2.0"

        filesToMoveToNewFolder.forEach {
            val folder = it.key
            val files = it.value
            outputString += newLine + if(folder.isDirectory && folder.exists()) {
                "Skipped creation of ${folder.name}-folder since it seems to already have happened"
            } else {
                folder.mkdirs()
                migrated = true
                "Created folder \"${folder.name}\""
            }
            var output = ""
            files.forEach { sourceFile ->
                output += if(sourceFile.exists()) {
                    val destinationFile = if(renameFiles.containsKey(sourceFile.name)) {
                        renameFiles[sourceFile.name]
                    } else {
                        sourceFile.name
                    }
                    sourceFile.copyTo(File("${folder.absolutePath}\\$destinationFile"))
                    sourceFile.delete()
                    migrated = true
                    "Moved file \"${sourceFile.name}\" to folder \"${folder.name}\"$newLine"
                } else {
                    "Skipped file \"${sourceFile.name}\"$newLine"
                }
            }
            outputString += output
        }


        filesToCreate.forEach {
            outputString += newLine + if(!it.exists()) {
                it.createNewFile()
                migrated = true
                "Created file \"${it.name}\"$newLine"
            } else {
                "Skipped creation of file \"${it.name}\" since it seems to already have happened$newLine"
            }
        }

        propertiesToMove.forEach{ (property, files) ->
            val sourceFile = files[0]
            val destinationFile = files[1]
            println(outputString)
            val sourceFileLines = sourceFile.readLines()

            outputString += newLine + if(sourceFileLines.any { it.startsWith(property) }) {
                val index = sourceFileLines.indexOfFirst { it.startsWith(property) }
                destinationFile.writeText(if(destinationFile.extension == "txt") {
                    sourceFileLines[index].substringAfter("=")
                } else {
                    sourceFileLines[index] + "\n"
                })

                sourceFile.writeText(sourceFileLines.filter { !it.contains(property) }.joinToString("\n"))
                migrated = true
                "Moved property \"$property\" from file \"$sourceFile\" to file \"$destinationFile\"$newLine"
            } else {
                "Skipped property \"$property\" cuz it does not exist in sourcefile \"$sourceFile\"$newLine"
            }

        }



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