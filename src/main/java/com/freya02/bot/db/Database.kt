package com.freya02.bot.db

import com.freya02.bot.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.SQLException

class Database(config: Config) {
    private val source: HikariDataSource

    init {
        val dbConfig = config.dbConfig

        val hikariConfig = HikariConfig().apply {
            jdbcUrl = dbConfig.dbURL
            username = dbConfig.user
            password = dbConfig.password

            maximumPoolSize = 2
            leakDetectionThreshold = 2500
        }

        source = HikariDataSource(hikariConfig)

        source.connection.close() //Test connection
    }

    val connection: Connection
        get() = try {
            source.connection
        } catch (e: SQLException) {
            throw RuntimeException("Unable to get a SQL connection", e)
        }
}