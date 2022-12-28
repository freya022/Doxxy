package com.freya02.bot.docs.mentions

import com.freya02.docs.DocSourceType
import java.util.*

data class ClassMention(val sourceType: DocSourceType, val identifier: String)

data class SimilarIdentifier(val sourceType: DocSourceType, val identifier: String, val similarity: Float) : Comparable<SimilarIdentifier> {
    fun isSimilarEnough() = similarity > 0.25

    private val comparator = Comparator.comparing(SimilarIdentifier::similarity)
        .reversed() //Reverse similarity order
        .thenComparing(SimilarIdentifier::identifier)

    override fun compareTo(other: SimilarIdentifier): Int {
        return comparator.compare(this, other)
    }
}

data class DocMatches(val classMentions: List<ClassMention>, val similarIdentifiers: SortedSet<SimilarIdentifier>) {
    fun isSufficient() = classMentions.isNotEmpty() || similarIdentifiers.any(SimilarIdentifier::isSimilarEnough)
}