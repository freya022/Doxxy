package dev.freya02.doxxy.bot.docs

import dev.freya02.doxxy.bot.docs.index.DocIndex
import dev.freya02.doxxy.docs.DocSourceType
import io.github.freya022.botcommands.api.core.db.Database
import io.github.freya022.botcommands.api.core.service.annotations.BService
import java.util.*

@BService
class DocIndexMap(database: Database) {
    private val indexMap = EnumMap<DocSourceType, DocIndex>(DocSourceType::class.java)

    init {
        for (value in DocSourceType.entries)
            indexMap[value] = DocIndex(value, database)
    }

    operator fun get(sourceType: DocSourceType) = indexMap[sourceType]
        ?: throw IllegalArgumentException("(somehow) Unknown source type: $sourceType")

    inline fun <R> withDocIndex(sourceType: DocSourceType, block: DocIndex.() -> R): R {
        return this[sourceType].let(block)
    }
}