package com.freya02.bot.db

import com.freya02.bot.utils.Utils.logQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.internal.utils.JDALogger
import java.sql.PreparedStatement

class KPreparedStatement(preparedStatement: PreparedStatement): PreparedStatement by preparedStatement {
    private fun setParameters(vararg params: Any?) {
        for ((i, param) in params.withIndex()) {
            setObject(i + 1, param)
        }
    }

    suspend fun execute(vararg params: Any?): Boolean = withContext(Dispatchers.IO) {
        setParameters(*params)
        logQuery()
        execute()
    }

    suspend fun executeUpdate(vararg params: Any?): Int = withContext(Dispatchers.IO) {
        setParameters(*params)
        logQuery()
        executeUpdate()
    }

    suspend inline fun executeReturningInsert(vararg params: Any?): DBResult = executeQuery(*params)

    suspend fun executeQuery(vararg params: Any?): DBResult = withContext(Dispatchers.IO) {
        setParameters(*params)
        logQuery()
        DBResult(executeQuery())
    }

    companion object {
        private val logger = JDALogger.getLog(KPreparedStatement::class.java)
    }
}