package com.freya02.bot.docs

import com.freya02.bot.db.Database
import com.freya02.bot.docs.index.DocIndexKt
import com.freya02.docs.DocSourceType
import java.io.IOException
import java.util.*

class DocIndexMap(database: Database) : EnumMap<DocSourceType, DocIndexKt>(DocSourceType::class.java) {
    init {
        this[DocSourceType.BOT_COMMANDS] = DocIndexKt(DocSourceType.BOT_COMMANDS, database)
        this[DocSourceType.JDA] = DocIndexKt(DocSourceType.JDA, database)
        this[DocSourceType.JAVA] = DocIndexKt(DocSourceType.JAVA, database)
    }

    @Synchronized
    @Throws(IOException::class)
    fun refreshAndInvalidateIndex(sourceType: DocSourceType) {
        this[sourceType]?.reindex()
    }
}