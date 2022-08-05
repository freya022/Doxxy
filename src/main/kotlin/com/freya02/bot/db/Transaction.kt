package com.freya02.bot.db

import org.intellij.lang.annotations.Language
import java.sql.Connection

class Transaction(val connection: Connection) {
    inline fun <R> preparedStatement(@Language("PostgreSQL") sql: String, block: KPreparedStatement.() -> R): R {
        return block(KPreparedStatement(connection.prepareStatement(sql)))
    }
}