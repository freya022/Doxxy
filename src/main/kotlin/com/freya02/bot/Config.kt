package com.freya02.bot

import com.google.gson.Gson
import io.github.freya022.botcommands.api.core.service.annotations.BService
import mu.KotlinLogging
import kotlin.io.path.readText

data class DBConfig(val serverName: String, val portNumber: Int, val user: String, val password: String, val dbName: String) {
    val dbURL: String
        get() = "jdbc:postgresql://$serverName:$portNumber/$dbName"
}

@BService
data class Config(val token: String,
                  val ownerIds: List<Long>,
                  val prefixes: List<String>,
                  val testGuildIds: List<Long>,
                  val fakeJDAGuildId: Long,
                  val fakeBCGuildId: Long,
                  val usePullUpdater: Boolean,
                  val pullUpdaterBaseUrl: String,
                  val pullUpdaterToken: String,
                  val dbConfig: DBConfig) {
    companion object {
        private val logger = KotlinLogging.logger { }

        @get:BService
        val config: Config by lazy {
            val configPath = Data.getEffectiveConfigPath()
            if (Data.isDevEnvironment) {
                logger.info("Loading test config")
            }

            return@lazy Gson().fromJson(configPath.readText(), Config::class.java)
        }
    }
}
