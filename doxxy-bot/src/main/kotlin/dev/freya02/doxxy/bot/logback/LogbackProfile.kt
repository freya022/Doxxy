package dev.freya02.doxxy.bot.logback

enum class LogbackProfile(val pathFragment: String) {
    DEV("dev/console"),
    DEV_WITH_FILE("dev/file"),
    PROD_WITH_FILE("prod/file")
}