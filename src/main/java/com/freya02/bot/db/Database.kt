package com.freya02.bot.db

import com.freya02.bot.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.intellij.lang.annotations.Language
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

    fun fetchConnection(): Connection = try {
        source.connection
    } catch (e: SQLException) {
        throw RuntimeException("Unable to get a SQL connection", e)
    }

    suspend fun <R> transactional(block: suspend Transaction.() -> R): R {
        val connection = fetchConnection()

        try {
            connection.autoCommit = false

            return block(Transaction(connection)).also { connection.commit() }
        } catch (e: Throwable) {
            connection.rollback()
            throw e
        } finally {
            connection.autoCommit = true
            connection.close()
        }
    }

    suspend fun <R> preparedStatement(@Language("PostgreSQL") sql: String, block: suspend KPreparedStatement.() -> R): R {
        return fetchConnection().use { connection -> block(KPreparedStatement(connection.prepareStatement(sql))) }
    }
}