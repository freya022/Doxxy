package com.freya02.bot.docs

import com.freya02.bot.docs.index.DocIndex
import com.freya02.docs.DocSourceType
import io.github.freya022.botcommands.api.core.db.Database
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.opentelemetry.api.OpenTelemetry
import java.util.*

@BService
class DocIndexMap(database: Database, openTelemetry: OpenTelemetry) {
    private val indexMap = EnumMap<DocSourceType, DocIndex>(DocSourceType::class.java)

    init {
        for (value in DocSourceType.entries)
            indexMap[value] = DocIndex(value, database, openTelemetry)
    }

    operator fun get(sourceType: DocSourceType) = indexMap[sourceType]
        ?: throw IllegalArgumentException("(somehow) Unknown source type: $sourceType")

    inline fun <R> withDocIndex(sourceType: DocSourceType, block: DocIndex.() -> R): R {
        return this[sourceType].let(block)
    }
}