package com.freya02.bot.docs

import com.freya02.bot.db.Database
import com.freya02.bot.docs.index.DocIndex
import com.freya02.docs.DocSourceType
import java.io.IOException
import java.util.*

class DocIndexMap(database: Database) : EnumMap<DocSourceType, DocIndex>(DocSourceType::class.java) {
    init {
        this[DocSourceType.BOT_COMMANDS] = DocIndex(DocSourceType.BOT_COMMANDS, database)
        this[DocSourceType.JDA] = DocIndex(DocSourceType.JDA, database)
        this[DocSourceType.JAVA] = DocIndex(DocSourceType.JAVA, database)
    }

    @Throws(IOException::class)
    suspend fun refreshAndInvalidateIndex(sourceType: DocSourceType) {
        this[sourceType]?.reindex()
    }
}