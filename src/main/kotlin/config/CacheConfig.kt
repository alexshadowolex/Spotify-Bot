package config

import getPropertyValue
import logger
import java.io.File
import java.io.FileOutputStream
import java.util.*

object CacheConfig {
    private val cacheConfigFile = File("data\\properties\\cacheConfig.properties")
    private val properties = Properties().apply {
        if(!cacheConfigFile.exists()) {
            logger.warn(
                "Error while reading property file ${cacheConfigFile.path} in CacheConfig init: " +
                        "File does not exist!"
            )

            cacheConfigFile.createNewFile()
            logger.info("Created new file ${cacheConfigFile.path}")
        } else
        load(cacheConfigFile.inputStream())
    }


    // Here


    private fun savePropertiesToFile() {
        try {
            properties.store(FileOutputStream(cacheConfigFile.path), null)
        } catch (e: Exception) {
            logger.error("Error while saving properties to file in BotConfig.")
            logger.error(e.stackTraceToString())
        }
    }
}