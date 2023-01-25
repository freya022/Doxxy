package com.freya02.bot.db

import com.freya02.bot.Config
import com.freya02.botcommands.api.core.ServiceStart
import com.freya02.botcommands.api.core.annotations.BService
import com.freya02.botcommands.api.core.annotations.ServiceType
import com.freya02.botcommands.api.core.db.ConnectionSupplier
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import kotlin.time.Duration.Companion.seconds

@BService(ServiceStart.PRE_LOAD)
@ServiceType(type = ConnectionSupplier::class)
class DatabaseSource(config: Config) : ConnectionSupplier {
    private val version = "2.0" //Same version as in CreateDatabase.sql
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

        checkVersion()
    }

    private fun checkVersion() {
        source.connection.use { connection ->
            connection.prepareStatement("select version from doxxy_version").use { statement ->
                statement.executeQuery().use { rs ->
                    if (!rs.next()) throw IllegalStateException("Found no version in database")

                    val dbVersion = rs.getString("version")
                    if (dbVersion != version) {
                        throw IllegalStateException("Database version mismatch, expected $version, database version is $dbVersion")
                    }
                }
            }
        }
    }

    override fun getConnection(): Connection = source.connection
}
