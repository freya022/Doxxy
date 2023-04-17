package com.freya02.bot.versioning

enum class VersionType(val fileName: String) {
    BotCommands("BC.txt"),
    JDAOfBotCommands("JDA_from_BC.txt"),
    JDA("JDA.txt"),
    JDAKTX("JDA-KTX.txt"),
    LAVAPLAYER("LavaPlayer.txt"),
    ;
}
