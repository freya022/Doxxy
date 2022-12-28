package com.freya02.bot.db

import com.freya02.bot.Config
import com.freya02.botcommands.api.core.annotations.BService
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.SQLException
import kotlin.time.Duration.Companion.seconds

@BService
class DatabaseSource(config: Config) {
    private val source: HikariDataSource

    init {
        val dbConfig = config.dbConfig

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = dbConfig.dbURL
            username = dbConfig.user
            password = dbConfig.password

            maximumPoolSize = 2
            leakDetectionThreshold = 10.seconds.inWholeMilliseconds
        }

        source = HikariDataSource(hikariConfig)

        source.connection.close() //Test connection
    }

    fun fetchConnection(): Connection = try {
        source.connection
    } catch (e: SQLException) {
        throw RuntimeException("Unable to get a SQL connection", e)
    }
}