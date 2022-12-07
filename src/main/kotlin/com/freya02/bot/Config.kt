package com.freya02.bot

import com.freya02.botcommands.api.Logging
import com.google.gson.Gson
import kotlin.io.path.exists
import kotlin.io.path.readText

data class DBConfig(val serverName: String, val portNumber: Int, val user: String, val password: String, val dbName: String) {
    val dbURL: String
        get() = "jdbc:postgresql://$serverName:$portNumber/$dbName"
}

data class Config(val token: String, val dbConfig: DBConfig) {
    companion object {
        private val logger = Logging.getLogger()

        val config: Config by lazy {
            val configPath = when {
                Data.testConfigPath.exists() -> {
                    logger.info("Loading test config")
                    Data.testConfigPath //Target folder prob IJ
                }

                else -> Data.configPath
            }

            Gson().fromJson(configPath.readText(), Config::class.java)
        }
    }
}