package dev.freya02.doxxy.common

// Not all of these are linkable (such as JDA-KTX and LavaPlayer)
enum class ExampleLibrary(val documentedLibrary: DocumentedExampleLibrary?) {
    JDA(DocumentedExampleLibrary.JDA),
    JDK(DocumentedExampleLibrary.JDK),
    BOT_COMMANDS(null),
    JDA_KTX(null),
    LAVA_PLAYER(null)
}

enum class DocumentedExampleLibrary {
    JDA,
    JDK
}