package com.freya02.bot.docs.index

import com.freya02.bot.docs.cached.CachedClass
import com.freya02.bot.docs.cached.CachedDoc
import com.freya02.bot.docs.cached.CachedField
import com.freya02.bot.docs.cached.CachedMethod

interface IDocIndex {
    suspend fun hasClassDoc(className: String): Boolean

    suspend fun getClassDoc(className: String): CachedClass?

    suspend fun getMethodDoc(className: String, identifier: String): CachedMethod?

    suspend fun getMethodDoc(fullSignature: String): CachedMethod? {
        val split = fullSignature.split("#")
        return when {
            split.size != 2 -> null
            else -> getMethodDoc(split[0], split[1])
        }
    }

    suspend fun getFieldDoc(className: String, fieldName: String): CachedField?

    suspend fun getFieldDoc(fullSignature: String): CachedField? {
        val split = fullSignature.split("#")
        return when {
            split.size != 2 -> null
            else -> getFieldDoc(split[0], split[1])
        }
    }

    suspend fun findAnySignatures(query: String, limit: Int = 25, docTypes: DocTypes): List<DocSearchResult>

    suspend fun findSignaturesIn(className: String, query: String? = null, docTypes: DocTypes, limit: Int = 25): List<DocSearchResult>
    suspend fun findMethodAndFieldSignaturesIn(className: String, query: String? = null, limit: Int = 25) =
        findSignaturesIn(className, query, DocTypes.IDENTIFIERS, limit = limit)

    suspend fun search(query: String): List<DocSearchResult>

    suspend fun getClasses(query: String? = null, limit: Int = 25): List<String>

    suspend fun resolveDoc(query: String): CachedDoc?
    suspend fun resolveDocAutocomplete(query: String): List<DocResolveResult>
}
