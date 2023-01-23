package com.freya02.bot

import com.freya02.botcommands.api.core.annotations.BService
import com.freya02.botcommands.api.core.suppliers.annotations.InstanceSupplier
import com.google.gson.Gson
import mu.KotlinLogging
import kotlin.io.path.readText

data class DBConfig(val serverName: String, val portNumber: Int, val user: String, val password: String, val dbName: String) {
    val dbURL: String
        get() = "jdbc:postgresql://$serverName:$portNumber/$dbName"
}

@BService
data class Config(val token: String, val dbConfig: DBConfig) {
    companion object {
        private val logger = KotlinLogging.logger { }

        @InstanceSupplier
        fun supply(): Config {
            val configPath = Data.getEffectiveConfigPath()
            if (Data.isDevEnvironment) {
                logger.info("Loading test config")
            }

            return Gson().fromJson(configPath.readText(), Config::class.java)
        }
    }
}
