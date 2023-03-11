package com.freya02.bot.docs.index

enum class DocType(val id: Int, val raw: Long) {
    CLASS(1, 1 shl 0),
    METHOD(2, 1 shl 1),
    FIELD(3, 1 shl 2)
}
