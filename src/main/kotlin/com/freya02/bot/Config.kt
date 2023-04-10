package com.freya02.bot

import com.freya02.botcommands.api.core.annotations.BService
import com.freya02.botcommands.api.core.suppliers.annotations.InstanceSupplier
import com.google.gson.Gson
import mu.KotlinLogging
import kotlin.io.path.readText

class DBConfig(
    val serverName: String,
    val portNumber: Int,
    val user: String,
    val password: String,
    val dbName: String
) {
    val dbURL: String
        get() = "jdbc:postgresql://$serverName:$portNumber/$dbName"
}

@BService
class Config(
    val token: String,
    val ownerIds: List<Long>,
    val prefixes: List<String>,
    val testGuildIds: List<Long>,
    val fakeJDAGuildId: Long,
    val fakeBCGuildId: Long,
    val dbConfig: DBConfig
) {
    companion object {
        private val logger = KotlinLogging.logger { }
        val config: Config by lazy {
            val configPath = Data.getEffectiveConfigPath()
            if (Data.isDevEnvironment) {
                logger.info("Loading test config")
            }

            return@lazy Gson().fromJson(configPath.readText(), Config::class.java)
        }

        @InstanceSupplier
        fun supply(): Config = config
    }
}
