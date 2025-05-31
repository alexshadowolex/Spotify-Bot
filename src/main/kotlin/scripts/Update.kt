package scripts

import java.io.File
import java.net.URL
import java.nio.file.Paths
import kotlin.system.exitProcess

const val UPDATE_SCRIPT_VERSION = "v1"
const val SPOTIFY_BOT_SUBSTRING = "Spotify"
const val UPDATE_PROPERTIES_SUBSTRING = "UpdateProperties"
const val UPDATE_ORDER_NAME = "UpdateOrder.config"
var updateOrder = listOf<String>()

// Compile with: kotlinc Update.kt -include-runtime -d Update_v1.jar

fun main(args: Array<String>) {
    val newVersion = args[0]
    val gitHubReleaseAssets = args[1].split(";").map {
        GitHubReleaseAsset(
            name = it.split(",")[0],
            browser_download_url = it.split(",")[1]
        )
    }
    println("Update-script version $UPDATE_SCRIPT_VERSION")
    println("Starting update to Spotify-Bot version $newVersion")

    val tempFolder = File("temp")
    if(!tempFolder.exists() || !tempFolder.isDirectory) {
        tempFolder.mkdir()
    }

    val updateDoneFile = File(tempFolder.path + "\\UPDATE_DONE.txt")
    if(updateDoneFile.exists()) {
        updateDoneFile.delete()
    }
    updateDoneFile.createNewFile()

    println("Starting the downloads of the release assets")

    val localReleaseAssets = mutableListOf<LocalReleaseAsset>()

    gitHubReleaseAssets.forEach { gitHubReleaseAsset ->
        var baseDir = ""
        val url = URL(gitHubReleaseAsset.browser_download_url)
        val assetData = url.readBytes()
        if(gitHubReleaseAsset.name == UPDATE_ORDER_NAME) {
            baseDir = tempFolder.path + "\\"
        }

        val assetFile = File("$baseDir${gitHubReleaseAsset.name}")

        assetFile.writeBytes(assetData)
        localReleaseAssets.add(LocalReleaseAsset(gitHubReleaseAsset.name, assetFile))
        println("Downloaded asset \"${gitHubReleaseAsset.name}\"")
    }

    println("Finished the downloads of the release assets")

    if(localReleaseAssets.any { it.name == UPDATE_ORDER_NAME }) {
        updateOrder = localReleaseAssets.find { it.name == UPDATE_ORDER_NAME }!!.localFile
            .readText().split("\n")
    }

    if(localReleaseAssets.any{ it.name.contains(UPDATE_PROPERTIES_SUBSTRING)} && updateOrder.isEmpty()) {
        updateOrder = listOf(UPDATE_PROPERTIES_SUBSTRING)
    }

    println("Given update order: ${updateOrder.joinToString(" -> ")}")

    updateOrder.forEach { currentFileName ->
        val currentAsset = localReleaseAssets.find { it.name.contains(currentFileName) }!!

        println("Executing ${currentAsset.name}")

        ProcessBuilder(
            listOf(
                "cmd", "/c",
                "javaw -jar ${currentAsset.localFile.name} autoUpdate"
            )
        ).start().waitFor()

        println("Finished execution of ${currentAsset.name}")
    }

    var checks = 0
    val maxChecks = 10
    var isUpdateDoneWithSuccess = false
    do {
        val doneFileContent = updateDoneFile.readText()
        if(updateOrder.none { !doneFileContent.contains(it) }) {
            isUpdateDoneWithSuccess = true
            break
        }

        checks++
        Thread.sleep(2000)
    } while (checks < maxChecks)

    if(!isUpdateDoneWithSuccess) {
        println("Error while auto updating")
        exitProcess(-1)
    }

    println("Executed all update assets. Cleaning up temp-files now")
    println("Deleting temp-folder")

    tempFolder.deleteRecursively()

    println("Deleting all assets not located in the temp-folder")

    val path = Paths.get("").toAbsolutePath().toString()
    val currentDir = File(path)
    currentDir.listFiles().filter { file ->
        localReleaseAssets
            .map { it.name }
            .filter { !it.contains(UPDATE_PROPERTIES_SUBSTRING) && !it.contains(SPOTIFY_BOT_SUBSTRING) }
            .contains(file.name)
    }.forEach {
        it.delete()
    }

    println("Deleting old versions of $UPDATE_PROPERTIES_SUBSTRING and $SPOTIFY_BOT_SUBSTRING")
    val spotifyBotAsset = localReleaseAssets.find { it.name.contains(SPOTIFY_BOT_SUBSTRING) }!!
    currentDir.listFiles().filter {
        it.name.contains(SPOTIFY_BOT_SUBSTRING) &&
        it.name != spotifyBotAsset.localFile.name &&
        it.extension == spotifyBotAsset.localFile.extension
    }.forEach {
        it.delete()
    }

    val updatePropertiesAsset = localReleaseAssets.find { it.name.contains(UPDATE_PROPERTIES_SUBSTRING) }!!
    if(localReleaseAssets.find{ it.name.contains(UPDATE_PROPERTIES_SUBSTRING) } != null) {
        currentDir.listFiles().filter {
            it.name.contains(UPDATE_PROPERTIES_SUBSTRING) &&
            it.name != updatePropertiesAsset.localFile.name &&
            it.extension == updatePropertiesAsset.localFile.extension
        }.forEach {
            it.delete()
        }
    }

    println("Update finished. Starting Spotify-Bot")

    ProcessBuilder(
        listOf(
            "cmd", "/c",
            "javaw -jar ${localReleaseAssets.find { it.name.contains(SPOTIFY_BOT_SUBSTRING) }!!.name} &"
        )
    ).start()

    println("Closing all CMD windows in 10s")
    Thread.sleep(10000)

    ProcessBuilder(
        listOf(
            "taskkill", "/IM", "cmd.exe"
        )
    ).start().waitFor()
}

data class LocalReleaseAsset (
    val name: String,
    val localFile: File
)

data class GitHubReleaseAsset (
    val name: String,
    val browser_download_url: String
)
