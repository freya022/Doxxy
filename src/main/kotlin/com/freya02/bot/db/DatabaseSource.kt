package com.freya02.bot.db

import com.freya02.bot.config.Config
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.freya022.botcommands.api.core.db.HikariSourceSupplier
import io.github.freya022.botcommands.api.core.service.annotations.BService
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.time.Duration.Companion.seconds

@BService
class DatabaseSource(config: Config) : HikariSourceSupplier {
    private val version = "2.5" //Same version as in CreateDatabase.sql
    override val source: HikariDataSource = HikariDataSource(HikariConfig().apply {
        val databaseConfig = config.databaseConfig
        jdbcUrl = databaseConfig.url
        username = databaseConfig.user
        password = databaseConfig.password

        maximumPoolSize = 2
        leakDetectionThreshold = 10.seconds.inWholeMilliseconds
    })

    private val migrationNameRegex = Regex("""v(\d+)\.(\d+)__.+\.sql""")
    private val dbVersionRegex = Regex("""(\d+)\.(\d+)""")

    init {
        checkVersion()
    }

    private fun checkVersion() {
        source.connection.use { connection ->
            connection.prepareStatement("select version from doxxy_version").use { statement ->
                statement.executeQuery().use { rs ->
                    if (!rs.next()) throw IllegalStateException("Found no version in database, please refer to the README to set up the database")

                    val dbVersion = rs.getString("version")
                    if (dbVersion != version) {
                        val sqlFolderPath = Path("sql")
                        val suffix = when {
                            sqlFolderPath.exists() -> buildHintSuffix(sqlFolderPath, dbVersion)
                            else -> ""
                        }

                        throw IllegalStateException("Database version mismatch, expected $version, database version is $dbVersion $suffix")
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun buildHintSuffix(sqlFolderPath: Path, dbVersion: String): String {
        val hintFiles = sqlFolderPath.walk()
            .filter { it.extension == "sql" }
            .filter {
                val (_, major, minor) = migrationNameRegex.matchEntire(it.name)?.groupValues ?: return@filter false
                val (_, dbMajor, dbMinor) = dbVersionRegex.matchEntire(dbVersion)?.groupValues ?: return@filter false

                //Keep if db version is lower than file
                if (dbMajor.toInt() < major.toInt()) return@filter true
                if (dbMinor.toInt() < minor.toInt()) return@filter true

                return@filter false
            }
            .joinToString { it.name }

        if (hintFiles.isBlank()) return ""
        return "\nHint: You should run the following migration scripts: $hintFiles"
    }
}
