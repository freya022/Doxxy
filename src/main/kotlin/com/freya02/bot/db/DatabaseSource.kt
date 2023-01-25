package com.freya02.bot.db

import com.freya02.bot.Config
import com.freya02.botcommands.api.core.ServiceStart
import com.freya02.botcommands.api.core.annotations.BService
import com.freya02.botcommands.api.core.annotations.ServiceType
import com.freya02.botcommands.api.core.db.ConnectionSupplier
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.nio.file.Path
import java.sql.Connection
import kotlin.io.path.*
import kotlin.time.Duration.Companion.seconds

@BService(ServiceStart.PRE_LOAD)
@ServiceType(type = ConnectionSupplier::class)
class DatabaseSource(config: Config) : ConnectionSupplier {
    private val version = "2.0" //Same version as in CreateDatabase.sql
    private val source: HikariDataSource

    private val migrationNameRegex = Regex("""v(\d+)\.(\d+)__.+\.sql""")
    private val dbVersionRegex = Regex("""(\d+)\.(\d+)""")

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

    @OptIn(ExperimentalPathApi::class)
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
        return buildString {
            append("\nHint: You should run the following migration scripts: $hintFiles")
        }
    }

    override fun getConnection(): Connection = source.connection
}
