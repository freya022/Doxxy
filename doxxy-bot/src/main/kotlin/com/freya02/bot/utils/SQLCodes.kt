package com.freya02.bot.utils

import java.sql.SQLException

const val UNIQUE_VIOLATION = "23505"

fun SQLException.isUniqueViolation(): Boolean {
    return sqlState == UNIQUE_VIOLATION
}