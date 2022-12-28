package com.freya02.bot.docs.mentions

import com.freya02.docs.DocSourceType
import java.util.*

data class ClassMention(val sourceType: DocSourceType, val identifier: String)

data class SimilarIdentifier(val sourceType: DocSourceType, val identifier: String, val similarity: Float) : Comparable<SimilarIdentifier> {
    fun isSimilarEnough() = similarity > 0.25

    //Reverse order
    override fun compareTo(other: SimilarIdentifier): Int = -similarity.compareTo(other.similarity)
}

data class DocMatches(val classMentions: List<ClassMention>, val similarIdentifiers: SortedSet<SimilarIdentifier>) {
    fun isSufficient() = classMentions.isNotEmpty() || similarIdentifiers.any(SimilarIdentifier::isSimilarEnough)
}