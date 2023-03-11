package com.freya02.bot.docs.mentions

import com.freya02.docs.DocSourceType
import java.util.*

data class ClassMention(val sourceType: DocSourceType, val identifier: String)

data class SimilarIdentifier(val sourceType: DocSourceType, val fullIdentifier: String, val fullHumanIdentifier: String, val similarity: Float) : Comparable<SimilarIdentifier> {
    private val comparator = Comparator.comparing(SimilarIdentifier::similarity)
        .reversed() //Reverse similarity order
        .thenComparing(SimilarIdentifier::fullIdentifier)

    override fun compareTo(other: SimilarIdentifier): Int {
        return comparator.compare(this, other)
    }
}

data class DocMatches(val classMentions: List<ClassMention>, val similarIdentifiers: SortedSet<SimilarIdentifier>) {
    val isEmpty = classMentions.isEmpty() && similarIdentifiers.isEmpty()
}