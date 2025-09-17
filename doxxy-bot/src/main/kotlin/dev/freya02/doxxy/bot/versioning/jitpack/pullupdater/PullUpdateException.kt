package dev.freya02.doxxy.bot.versioning.jitpack.pullupdater

class PullUpdateException(
    val type: ExceptionType,
    message: String
) : Exception(
    "[$type]: $message",
    null,
    true,
    false
) {
    enum class ExceptionType {
        UNKNOWN_ERROR,
        UNSUPPORTED_LIBRARY,
        PR_UPDATE_FAILURE,
        HEAD_REF_NOT_FOUND,
        BASE_REF_NOT_FOUND
    }
}
