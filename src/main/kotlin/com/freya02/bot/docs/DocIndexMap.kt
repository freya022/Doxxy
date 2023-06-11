package com.freya02.bot.docs

import com.freya02.bot.docs.index.DocIndex
import com.freya02.bot.docs.index.ReindexData
import com.freya02.botcommands.api.core.db.Database
import com.freya02.botcommands.api.core.service.annotations.BService
import com.freya02.docs.DocSourceType
import java.io.IOException
import java.util.*

@BService
class DocIndexMap(database: Database) : EnumMap<DocSourceType, DocIndex>(DocSourceType::class.java) {
    init {
        this[DocSourceType.BOT_COMMANDS] = DocIndex(DocSourceType.BOT_COMMANDS, database)
        this[DocSourceType.JDA] = DocIndex(DocSourceType.JDA, database)
        this[DocSourceType.JAVA] = DocIndex(DocSourceType.JAVA, database)
    }

    @Throws(IOException::class)
    suspend fun refreshAndInvalidateIndex(sourceType: DocSourceType, reindexData: ReindexData) {
        this[sourceType]?.reindex(reindexData)
    }
}