package com.freya02.bot.db

import com.freya02.bot.utils.Utils.logQuery
import com.freya02.botcommands.api.Logging
import com.freya02.botcommands.internal.utils.ReflectionUtils
import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException

class DBAction private constructor(
    val connection: Connection,
    val preparedStatement: PreparedStatement,
    private val shouldReturnData: Boolean
) : AutoCloseable {
    @Throws(SQLException::class)
    fun executeQuery(vararg parameters: Any?): DBResult {
        for ((i, parameter) in parameters.withIndex()) {
            preparedStatement.setObject(i + 1, parameter)
        }

        if (!shouldReturnData) {
            LOGGER.warn(
                "Call at {} asks for data to be queried but no column names as been specified, this is just a performance issue",
                ReflectionUtils.formatCallerMethod()
            )
        }
        preparedStatement.logQuery()

        return DBResult(preparedStatement.executeQuery())
    }

    /**
     * @see PreparedStatement.executeUpdate
     */
    @Throws(SQLException::class)
    fun executeUpdate(vararg parameters: Any?): Int {
        for ((i, parameter) in parameters.withIndex()) {
            preparedStatement.setObject(i + 1, parameter)
        }
        preparedStatement.logQuery()

        return preparedStatement.executeUpdate()
    }

    @Throws(SQLException::class)
    override fun close() {
        connection.close()
    }

    companion object {
        private val LOGGER = Logging.getLogger()

        @Throws(SQLException::class)
        fun of(database: Database, @Language("PostgreSQL") statement: String): DBAction {
            val connection = database.fetchConnection()
            return DBAction(connection, connection.prepareStatement(statement), false)
        }

        @Throws(SQLException::class)
        fun of(
            database: Database,
            @Language("PostgreSQL") statement: String,
            vararg returnedColumnIndexes: Int
        ): DBAction {
            val connection = database.fetchConnection()
            return DBAction(connection, connection.prepareStatement(statement, returnedColumnIndexes), true)
        }

        @Throws(SQLException::class)
        fun of(
            database: Database,
            @Language("PostgreSQL") statement: String,
            vararg returnedColumnNames: String
        ): DBAction {
            val connection = database.fetchConnection()
            return DBAction(connection, connection.prepareStatement(statement, returnedColumnNames), true)
        }
    }
}