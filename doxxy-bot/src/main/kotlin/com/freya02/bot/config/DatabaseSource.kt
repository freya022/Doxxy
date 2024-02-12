package com.freya02.bot.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.freya022.botcommands.api.core.db.HikariSourceSupplier
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.Flyway
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger { }

@BService
class DatabaseSource(config: Config) : HikariSourceSupplier {
    override val source: HikariDataSource = HikariDataSource(HikariConfig().apply {
        val databaseConfig = config.databaseConfig
        jdbcUrl = databaseConfig.url
        username = databaseConfig.user
        password = databaseConfig.password

        maximumPoolSize = 2
        leakDetectionThreshold = 10.seconds.inWholeMilliseconds
    })

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
}
