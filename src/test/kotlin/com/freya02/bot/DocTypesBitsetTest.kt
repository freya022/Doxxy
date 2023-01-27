package com.freya02.bot

import com.freya02.bot.docs.index.DocType
import com.freya02.bot.docs.index.DocTypes
import java.util.*

object DocTypesBitsetTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val raw = DocTypes(EnumSet.of(DocType.METHOD, DocType.FIELD)).getRaw()
        println(java.lang.Long.toBinaryString(raw))
        val fromRaw = DocTypes.fromRaw(raw)
        println(DocTypes(EnumSet.of(DocType.METHOD, DocType.FIELD)) == fromRaw)
    }
}
