package dev.freya02.doxxy.bot.utils

import java.sql.SQLException

const val UNIQUE_VIOLATION = "23505"

fun SQLException.isUniqueViolation(): Boolean {
    return sqlState == UNIQUE_VIOLATION
}