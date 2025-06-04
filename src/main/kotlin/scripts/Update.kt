package scripts

import java.io.File
import java.net.URL
import java.nio.file.Paths
import kotlin.system.exitProcess

const val UPDATE_SCRIPT_VERSION = "v1"
const val SPOTIFY_BOT_SUBSTRING = "Spotify"
const val UPDATE_PROPERTIES_SUBSTRING = "UpdateProperties"
const val UPDATE_ORDER_NAME = "UpdateOrder.config"
val CURRENT_DIR = File(Paths.get("").toAbsolutePath().toString())
var updateOrder = listOf<String>()

// Compile with: kotlinc Update.kt -include-runtime -d Update_v1.jar

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Not enough arguments given. Usage: <version> <assetList>")
        exitProcess(-1)
    }

    val newVersion = args[0]
    val gitHubReleaseAssets = parseGitHubAssets(args[1])

    println("Update-script version $UPDATE_SCRIPT_VERSION")
    println("Starting update to Spotify-Bot version $newVersion")

    val tempFolder = File("temp").apply { mkdir() }

    println("Starting the downloads of the release assets")

    val localReleaseAssets = downloadAssets(gitHubReleaseAssets, tempFolder)
    println("Finished the downloads of the release assets")

    updateOrder = determineUpdateOrder(localReleaseAssets)

    println("Given update order: ${updateOrder.joinToString(" -> ")}")

    executeUpdateScripts(updateOrder, localReleaseAssets)

    println("Executed all update assets. Cleaning up temp-files now")

    cleanup(tempFolder, localReleaseAssets)

    println("Update finished. Starting Spotify-Bot")

    startSpotifyBot(localReleaseAssets)

    println("Console will close in 15s")
    Thread.sleep(15000)
}

fun parseGitHubAssets(raw: String): List<GitHubReleaseAsset> {
    return raw.split(";").map {
        val (name, url) = it.split(",")
        GitHubReleaseAsset(name.trim(), url.trim())
    }
}

fun downloadAssets(assets: List<GitHubReleaseAsset>, tempFolder: File): MutableList<LocalReleaseAsset> {
    val localAssets = mutableListOf<LocalReleaseAsset>()
    for (asset in assets) {
        val baseDir = if (asset.name == UPDATE_ORDER_NAME) {
            tempFolder
        } else {
            File("")
        }
        val file = File("${baseDir.path}${asset.name}")

        if(!asset.name.contains(UPDATE_PROPERTIES_SUBSTRING))
            file.writeBytes(URL(asset.browser_download_url).readBytes())
        localAssets += LocalReleaseAsset(asset.name, file)
        println("Downloaded asset \"${asset.name}\"")
    }
    return localAssets
}

fun determineUpdateOrder(assets: List<LocalReleaseAsset>): List<String> {
    val orderFile = assets.find { it.name == UPDATE_ORDER_NAME }?.localFile
    return when {
        orderFile != null -> orderFile.readLines().filter { it.isNotBlank() }
        assets.any { it.name.contains(UPDATE_PROPERTIES_SUBSTRING) } -> listOf(UPDATE_PROPERTIES_SUBSTRING)
        else -> emptyList()
    }
}

fun executeUpdateScripts(order: List<String>, assets: List<LocalReleaseAsset>) {
    for (step in order) {
        val asset = assets.find { it.name.contains(step) } ?: continue
        println("Executing ${asset.name}")
        ProcessBuilder("javaw", "-jar", asset.localFile.name)
            .inheritIO().start().waitFor()
        println("Finished execution of ${asset.name}")
    }
}

fun cleanup(tempFolder: File, assets: List<LocalReleaseAsset>) {
    println("Deleting temp-folder")

    tempFolder.deleteRecursively()

    val assetNames = assets.map { it.name }.toSet()

    println("Deleting all assets not located in the temp-folder")
    CURRENT_DIR.listFiles()?.filter { file ->
        assetNames.contains(file.name) &&
                !file.name.contains(SPOTIFY_BOT_SUBSTRING) &&
                !file.name.contains(UPDATE_PROPERTIES_SUBSTRING)
    }?.forEach { it.delete() }

    println("Deleting old versions of $UPDATE_PROPERTIES_SUBSTRING and $SPOTIFY_BOT_SUBSTRING")
    val spotifyBotAsset = assets.find { it.name.contains(SPOTIFY_BOT_SUBSTRING) }!!
    CURRENT_DIR.listFiles().filter {
        it.name.contains(SPOTIFY_BOT_SUBSTRING) &&
        it.name != spotifyBotAsset.localFile.name &&
        it.extension == spotifyBotAsset.localFile.extension
    }.forEach {
        it.delete()
    }

    val updatePropertiesAsset = assets.find { it.name.contains(UPDATE_PROPERTIES_SUBSTRING) }!!
    if(assets.find{ it.name.contains(UPDATE_PROPERTIES_SUBSTRING) } != null) {
        CURRENT_DIR.listFiles().filter {
            it.name.contains(UPDATE_PROPERTIES_SUBSTRING) &&
            it.name != updatePropertiesAsset.localFile.name &&
            it.extension == updatePropertiesAsset.localFile.extension
        }.forEach {
            it.delete()
        }
    }
}


fun startSpotifyBot(assets: List<LocalReleaseAsset>) {
    val bot = assets.find { it.name.contains(SPOTIFY_BOT_SUBSTRING) }?.name ?: return
    ProcessBuilder("javaw", "-jar", bot).start()
}


data class LocalReleaseAsset (
    val name: String,
    val localFile: File
)

data class GitHubReleaseAsset (
    val name: String,
    val browser_download_url: String
)
