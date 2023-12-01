package config

import java.io.File
import java.util.*

object BuildInfo {
    private val properties = Properties().apply {
        load(File("src\\main\\resources\\buildInfo.properties").inputStream())
    }

    val version: String = properties.getProperty("version")
}