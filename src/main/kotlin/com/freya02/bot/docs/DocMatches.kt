package com.freya02.bot.docs

import com.freya02.docs.DocSourceType

data class ClassMention(val sourceType: DocSourceType, val identifier: String)
data class SimilarIdentifier(val sourceType: DocSourceType, val identifier: String, val similarity: Float)
data class DocMatches(val classMentions: List<ClassMention>, val similarIdentifiers: List<SimilarIdentifier>) {
    val identicalIdentifiers: List<SimilarIdentifier>
        get() = similarIdentifiers.filter { it.similarity == 1.0f }

    fun isEmpty() = classMentions.isEmpty() && similarIdentifiers.isEmpty()
}