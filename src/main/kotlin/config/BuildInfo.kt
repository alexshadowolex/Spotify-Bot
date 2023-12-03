package config

import java.util.*

object BuildInfo {
    private val properties = Properties().apply {
        load(this@BuildInfo::class.java.getResourceAsStream("/buildInfo.properties"))
    }

    val version: String = properties.getProperty("version")
    var latestAvailableVersion: String = properties.getProperty("version")
}