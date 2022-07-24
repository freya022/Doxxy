package com.freya02.bot

import com.google.gson.Gson
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.readText

data class DBConfig(val serverName: String, val portNumber: Int, val user: String, val password: String, val dbName: String) {
    val dbURL: String
        get() = "jdbc:postgresql://$serverName:$portNumber/$dbName"
}

data class Config(val token: String, val dbConfig: DBConfig) {
    companion object {
        private var config_: Config? = null

        @Synchronized
        fun getConfig(): Config {
            if (config_ == null) {
                val classPath = Path.of(Main::class.java.protectionDomain.codeSource.location.toURI())
                val configPath = when {
                    classPath.isDirectory() -> Path.of("Test_Config.json") //Target folder prob IJ
                    else -> Path.of("Config.json")
                }

                config_ = Gson().fromJson(configPath.readText(), Config::class.java)
            }

            return config_!!
        }
    }
}