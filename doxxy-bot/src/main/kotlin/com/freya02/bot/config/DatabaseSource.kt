package com.freya02.bot.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.freya022.botcommands.api.core.db.ConnectionSupplier
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.sql.SQLException
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger { }

@BService
class DatabaseSource(config: Config, openTelemetry: OpenTelemetry) : ConnectionSupplier {
    private val hikariConfig = HikariConfig().apply {
        val databaseConfig = config.databaseConfig
        jdbcUrl = databaseConfig.url
        username = databaseConfig.user
        password = databaseConfig.password

        maximumPoolSize = 2
        leakDetectionThreshold = 10.seconds.inWholeMilliseconds
    }
    private val source = JdbcTelemetry.builder(openTelemetry).build().wrap(HikariDataSource(hikariConfig))

    init {
        //Migrate BC tables
        createFlyway("bc", "bc_database_scripts").migrate()

        createFlyway("public", "doxxy_database_scripts").migrate()

        logger.info { "Created database source" }
    }

    private fun createFlyway(schema: String, scriptsLocation: String): Flyway = Flyway.configure()
        .dataSource(source)
        .schemas(schema)
        .locations(scriptsLocation)
        .validateMigrationNaming(true)
        .loggers("slf4j")
        .load()

    override val maxConnections: Int
        get() = hikariConfig.maximumPoolSize
    override val maxTransactionDuration: Duration
        get() = Duration.ofMillis(hikariConfig.leakDetectionThreshold)

    @Throws(SQLException::class)
    override fun getConnection(): Connection = source.connection
}
