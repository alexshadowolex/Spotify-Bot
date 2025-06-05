package scripts

import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

private const val UPDATE_SCRIPT_VERSION = "v1"
private const val SPOTIFY_BOT_SUBSTRING = "Spotify"
private const val UPDATE_PROPERTIES_SUBSTRING = "UpdateProperties"
private const val UPDATE_ORDER_NAME = "UpdateOrder.config"
private const val UPDATE_SCRIPT_SUBSTRING = "Update_v"

private val CURRENT_DIR = File(Paths.get("").toAbsolutePath().toString())
private const val TEMP_DIRECTORY = "temp"
private const val LOG_DIRECTORY = "logs\\update"
private const val DATA_DIRECTORY = "data"
private const val BACKUP_DIRECTORY = "$TEMP_DIRECTORY\\data_backup"

private var updateOrder = listOf<String>()

private val outputString = mutableListOf<String>()
// Compile with: kotlinc Update.kt -include-runtime -d Update_v1.jar

/**
 * This script will automatically update the Spotify-Bot. It is compiled into a standalone Jar that gets executed
 * by the Spotify-Bot after clicking on the Update-Button.
 * @param args Array with the newVersion on index 0 and all Spotify Assets (name, Download-URL) as a list on index 1
 */
fun main(args: Array<String>) {
    if (args.size < 2) {
        printAndLogMessage("Not enough arguments given. Usage: <version> <assetList>")
        logMessageToFile()
        exitProcess(-1)
    }

    val newVersion = args[0]
    val gitHubReleaseAssets = parseGitHubAssets(args[1])

    if(gitHubReleaseAssets == null) {
        printAndLogMessage("Error while parsing GitHub-assets command-line arguments")
        logMessageToFile()

        exitProcess(-1)
    }

    printAndLogMessage("Update-script version $UPDATE_SCRIPT_VERSION")
    printAndLogMessage("Starting update to Spotify-Bot version $newVersion")

    val tempFolder = File(TEMP_DIRECTORY).apply { mkdir() }

    printAndLogMessage("Starting the downloads of the release assets")

    val localReleaseAssets = downloadAssets(gitHubReleaseAssets, tempFolder)
    if(localReleaseAssets == null) {
        printAndLogMessage("Error while downloading GitHub-assets")
        logMessageToFile()

        exitProcess(-1)
    }
    printAndLogMessage("Finished the downloads of the release assets")

    updateOrder = determineUpdateOrder(localReleaseAssets)

    printAndLogMessage("Given update order: ${updateOrder.joinToString(" -> ")}")

    backupDataFolder()
    try {
        executeUpdateScripts(updateOrder, localReleaseAssets)
    } catch (e: Exception) {
        printAndLogMessage("Error during update: ${e.message}")
        printAndLogMessage("Attempting to start previous Spotify-Bot version")

        restoreDataFolderBackup()
        startOldSpotifyBot()
        logMessageToFile()

        exitProcess(-1)
    }

    printAndLogMessage("Executed all update assets. Cleaning up temp-files now")

    cleanup(tempFolder, localReleaseAssets)

    printAndLogMessage("Update finished. Starting Spotify-Bot")

    startSpotifyBot(localReleaseAssets)

    printAndLogMessage("Console will close in 15s")
    logMessageToFile()
    Thread.sleep(15000)
}


/**
 * Parses the raw String to GitHubReleaseAsset-objects.
 * @param raw the raw String
 * @return the list of the parsed GitHubReleaseAsset-objects, null on error
 */
fun parseGitHubAssets(raw: String): List<GitHubReleaseAsset>? = raw.split(";").mapNotNull {
    val parts = it.split(",")
    if(parts.size == 2) {
        val (name, url) = it.split(",")
        GitHubReleaseAsset(name.trim(), url.trim())
    } else {
        printAndLogMessage("Malformed asset definition: $it")
        null
    }
}


/**
 * Downloads the given assets from GitHub and saves them in the base-directory. If it is a file containing the
 * UPDATE_ORDER_NAME, it will be saved in the temp-folder.
 * @param assets the list of GitHubReleaseAsset-objects to be downloaded
 * @param tempFolder the temp-folder
 * @return list of the LocalReleaseAsset-objects
 */
fun downloadAssets(assets: List<GitHubReleaseAsset>, tempFolder: File): List<LocalReleaseAsset>? {
    val localAssets = mutableListOf<LocalReleaseAsset>()

    for (asset in assets) {
        if(asset.name.contains(UPDATE_SCRIPT_SUBSTRING)) {
            continue
        }
        val baseDir = if (asset.name == UPDATE_ORDER_NAME) {
            tempFolder
        } else {
            File("")
        }
        val file = File("${baseDir.path}${asset.name}")

        try {
            file.writeBytes(URL(asset.browser_download_url).readBytes())
            localAssets += LocalReleaseAsset(asset.name, file)
            printAndLogMessage("Downloaded asset \"${asset.name}\"")
        } catch (e: IOException) {
            printAndLogMessage("Error downloading ${asset.name}: ${e.message}")
            return null
        }
    }
    return localAssets
}


/**
 * Determines the update-order, depending on whether there is an asset containing the UPDATE_ORDER_NAME-String. If
 * there is no file like that, the update-order will only consist of the UpdateProperties-file if it exists as
 * an asset. If that is also not the case, the list will be empty.
 * @param assets the list of LocalReleaseAsset-objects
 * @return the update-order as a list of Strings
 */
fun determineUpdateOrder(assets: List<LocalReleaseAsset>): List<String> {
    val orderFile = assets.find { it.name == UPDATE_ORDER_NAME }?.localFile
    return when {
        orderFile != null -> orderFile.readLines().filter { it.isNotBlank() }
        assets.any { it.name.contains(UPDATE_PROPERTIES_SUBSTRING) } -> listOf(UPDATE_PROPERTIES_SUBSTRING)
        else -> emptyList()
    }
}


/**
 * Executes all update-scripts downloaded from GitHub. If a script returned with an error, it will be retried 3
 * times. If all of them were not executed successfully, the function throws a RuntimeException.
 * @param order the execution-order for the update-scripts
 * @param assets list of LocalReleaseAsset-objects
 */
fun executeUpdateScripts(order: List<String>, assets: List<LocalReleaseAsset>) {
    for (step in order) {
        val retries = 3
        val asset = assets.find { it.name.contains(step) } ?: continue
        printAndLogMessage("Executing ${asset.name}")
        var success = false
        for (attempt in 0..retries) {
            val exitCode = try {
                val process = ProcessBuilder("java", "-jar", asset.localFile.name, "autoUpdate")
                    .inheritIO().start()
                process.waitFor()
            } catch (e: IOException) {
                printAndLogMessage("Process start failed for ${asset.name}: ${e.message}")
                -1
            }

            if(exitCode == 0) {
                success = true
                break
            } else {
                printAndLogMessage("Attempt ${attempt + 1} for ${asset.name} failed with exit code $exitCode")
                Thread.sleep(2000)
            }

        }

        if (!success) {
            throw RuntimeException("Update step \"${asset.name}\" failed after $retries retries")
        }
        printAndLogMessage("Finished execution of ${asset.name}")
    }
}


/**
 * Deletes up all the one-time-update-scripts and the temp-folder. Then it deletes the older versions of the
 * UpdateProperties- and Spotify-Bot-Jar.
 * @param tempFolder the temp-folder
 * @param assets list of LocalReleaseAsset-objects
 */
fun cleanup(tempFolder: File, assets: List<LocalReleaseAsset>) {
    printAndLogMessage("Deleting temp-folder")

    tempFolder.deleteRecursively()

    val assetNames = assets.map { it.name }.toSet()

    printAndLogMessage("Deleting all assets not located in the temp-folder")
    CURRENT_DIR.listFiles()?.filter { file ->
        assetNames.contains(file.name) &&
                !file.name.contains(SPOTIFY_BOT_SUBSTRING) &&
                !file.name.contains(UPDATE_PROPERTIES_SUBSTRING)
    }?.forEach {
        val isDeleteSuccessful = it.delete()
        if(!isDeleteSuccessful) {
            printAndLogMessage("Warning: could not delete ${it.name}")
        }
    }

    printAndLogMessage("Deleting old versions of $UPDATE_PROPERTIES_SUBSTRING and $SPOTIFY_BOT_SUBSTRING")
    deleteOldVersions(
        SPOTIFY_BOT_SUBSTRING,
        assets.find { it.name.contains(SPOTIFY_BOT_SUBSTRING) }!!.localFile
    )

    deleteOldVersions(
        UPDATE_PROPERTIES_SUBSTRING,
        assets.find { it.name.contains(UPDATE_PROPERTIES_SUBSTRING) }!!.localFile
    )
}


/**
 * Deletes all old JAR files in the current directory that match the given identifier,
 * keeping only the specified current file.
 * This method looks for files whose names contain the given identifier and have the
 * same extension as the current file.
 * If a file matches but is not the current file, it will be deleted.
 * @param identifier A string used to identify which files should be considered for deletion.
 * @param currentFile The file that should be kept; all other matching files will be deleted.
 */
fun deleteOldVersions(identifier: String, currentFile: File) {
    CURRENT_DIR.listFiles()?.filter {
        it.name.contains(identifier) &&
                it.name != currentFile.name &&
                it.extension == currentFile.extension
    }?.forEach {
        val isDeleteSuccessful = it.delete()
        if(!isDeleteSuccessful) {
            printAndLogMessage("Warning: could not delete ${it.name}")
        }
    }
}


/**
 * Starts the old version of the Spotify-Bot. This is meant in case of an error while updating and has to be called
 * before the cleanup-function.
 */
fun startOldSpotifyBot() {
    val candidates = CURRENT_DIR.listFiles()?.filter {
        it.name.contains(SPOTIFY_BOT_SUBSTRING) && it.extension == "jar"
    } ?: return

    val mostRecentOld = candidates.sortedByDescending { it.lastModified() }.drop(1).firstOrNull() ?: return

    try {
        printAndLogMessage("Restarting old Spotify-Bot: ${mostRecentOld.name}")
        ProcessBuilder("javaw", "-jar", mostRecentOld.absolutePath).start()
    } catch (e: IOException) {
        printAndLogMessage("Failed to restart old bot: ${e.message}")
    }
}


/**
 * Starts the new version of the Spotify-Bot. This should only be called after successfully updating everything.
 * @param assets list of LocalReleaseAsset-objects
 */
fun startSpotifyBot(assets: List<LocalReleaseAsset>) {
    val bot = assets.find { it.name.contains(SPOTIFY_BOT_SUBSTRING) }?.name ?: return

    try {
        ProcessBuilder("javaw", "-jar", bot).start()
    } catch (e: IOException) {
        printAndLogMessage("Failed to start new bot: ${e.message}")
    }
}


/**
 * Prints the given message to console and writes it to the to be logged output-string.
 * @param message the message to print and log
 */
fun printAndLogMessage(message: String) {
    println(message)
    outputString += message
}


/**
 * Writes the content of the outputString-variable to the log-file.
 */
fun logMessageToFile() {
    Files.createDirectories(Paths.get(LOG_DIRECTORY))

    val logFileName = DateTimeFormatter
        .ISO_INSTANT
        .format(java.time.Instant.now())
        .replace(':', '-')

    Paths.get(LOG_DIRECTORY, "Update_${logFileName}.log").toFile().also {
        if (!it.exists()) {
            it.createNewFile()
        }
    }.writeText(outputString.joinToString("\n"))
}


/**
 * Backs up the data-directory to the temp folder.
 */
fun backupDataFolder() {
    printAndLogMessage("Backing up data-directory")
    val dataDirectory = File(DATA_DIRECTORY)
    val backupDirectory = File(BACKUP_DIRECTORY)

    if (!dataDirectory.exists() || !dataDirectory.isDirectory) {
        return
    }

    try {
        if (backupDirectory.exists()) {
            backupDirectory.deleteRecursively()
        }
        backupDirectory.mkdirs()

        dataDirectory.copyRecursively(backupDirectory, overwrite = true)
        printAndLogMessage("Finished backing up data-directory")
    } catch (e: IOException) {
        printAndLogMessage("backing up data-directory failed: ${e.message}")
    }
}


/**
 * Restores the backed-up data-folder.
 */
fun restoreDataFolderBackup() {
    printAndLogMessage("Restoring backed up data-directory")
    val dataDirectory = File(DATA_DIRECTORY)
    val backupDirectory = File(BACKUP_DIRECTORY)

    if (!backupDirectory.exists() || !backupDirectory.isDirectory) {
        return
    }

    try {
        if (dataDirectory.exists()) {
            dataDirectory.deleteRecursively()
        }
        dataDirectory.mkdirs()

        backupDirectory.copyRecursively(dataDirectory, overwrite = true)
        printAndLogMessage("Finished restoring backed up data-directory")
    } catch (e: IOException) {
        printAndLogMessage("Restoring backed up data-directory failed: ${e.message}")
    }
}


data class LocalReleaseAsset (
    val name: String,
    val localFile: File
)

data class GitHubReleaseAsset (
    val name: String,
    val browser_download_url: String
)
