package dev.freya02.doxxy.bot

import dev.freya02.doxxy.bot.docs.index.DocType
import dev.freya02.doxxy.bot.docs.index.DocTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.lang.Long.toBinaryString
import java.util.*

class DocTypesBitsetTest {
    @Test
    fun main() {
        assertEquals(DocTypes(EnumSet.of(DocType.METHOD, DocType.FIELD)), DocTypes.IDENTIFIERS)

        val raw = DocTypes.IDENTIFIERS.getRaw()
        assertEquals("110", toBinaryString(raw))

        val fromRaw = DocTypes.fromRaw(raw)
        assertEquals(DocTypes.IDENTIFIERS, fromRaw)
    }
}
