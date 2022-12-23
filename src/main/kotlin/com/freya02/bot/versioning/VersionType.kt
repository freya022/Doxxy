package com.freya02.bot.versioning

enum class VersionType(val fileName: String) {
    BotCommands("BC.txt"),
    JDAOfBotCommands("JDA_from_BC.txt"),
    JDA5("JDA5.txt"),
    JDAKTX("JDA-KTX.txt");
}