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

    override fun getConnection(): Connection = source.connection
}