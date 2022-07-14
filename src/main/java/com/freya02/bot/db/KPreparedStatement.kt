package com.freya02.bot.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.PreparedStatement

class KPreparedStatement(preparedStatement: PreparedStatement): PreparedStatement by preparedStatement {
    fun setParameters(vararg params: Any?) {
        for ((i, param) in params.withIndex()) {
            setObject(i + 1, param)
        }
    }

    suspend fun executeUpdate(vararg params: Any?) {
        withContext(Dispatchers.IO) {
            setParameters(*params)
            executeUpdate()
        }
    }

    suspend inline fun executeReturningInsert(vararg params: Any?): DBResult = executeQuery(*params)

    suspend fun executeQuery(vararg params: Any?): DBResult {
        return withContext(Dispatchers.IO) {
            setParameters(*params)
            DBResult(executeQuery())
        }
    }
}