package com.freya02.bot.versioning

enum class VersionType(val libraryType: LibraryType) {
    BOT_COMMANDS(LibraryType.BOT_COMMANDS),
    JDA_OF_BOT_COMMANDS(LibraryType.BOT_COMMANDS),
    JDA(LibraryType.JDA),
    JDA_KTX(LibraryType.JDA_KTX),
    LAVA_PLAYER(LibraryType.LAVA_PLAYER),
    ;
}
