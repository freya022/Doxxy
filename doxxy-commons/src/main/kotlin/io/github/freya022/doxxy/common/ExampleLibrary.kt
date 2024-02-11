package io.github.freya022.doxxy.common

// Not all of these are linkable (such as JDA-KTX and LavaPlayer)
enum class ExampleLibrary(val documentedLibrary: DocumentedExampleLibrary?) {
    JDA(DocumentedExampleLibrary.JDA),
    JDK(DocumentedExampleLibrary.JDK),
    BOT_COMMANDS(DocumentedExampleLibrary.BOT_COMMANDS),
    JDA_KTX(null),
    LAVA_PLAYER(null)
}

enum class DocumentedExampleLibrary {
    JDA,
    JDK,
    BOT_COMMANDS
}