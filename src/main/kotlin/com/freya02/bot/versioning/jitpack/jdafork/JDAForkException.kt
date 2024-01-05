package com.freya02.bot.versioning.jitpack.jdafork

class JDAForkException(
    val type: ExceptionType,
    message: String
) : Exception(
    "[$type]: $message",
    cause = null,
    enableSuppression = true,
    writableStackTrace = false
) {
    enum class ExceptionType {
        UNKNOWN_ERROR,
        UNSUPPORTED_LIBRARY,
        PR_NOT_FOUND,
        PR_UPDATE_FAILURE,
        HEAD_REF_NOT_FOUND,
        BASE_REF_NOT_FOUND
    }
}