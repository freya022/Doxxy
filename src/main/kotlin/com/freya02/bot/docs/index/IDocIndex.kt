package com.freya02.bot.docs.index

import com.freya02.bot.docs.cached.CachedClass
import com.freya02.bot.docs.cached.CachedDoc
import com.freya02.bot.docs.cached.CachedField
import com.freya02.bot.docs.cached.CachedMethod

interface IDocIndex {
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

    suspend fun findAnySignatures(query: String?, limit: Int = 25, vararg docTypes: DocType): List<DocSearchResult>
    suspend fun findAnyMethodSignatures(query: String? = null, limit: Int = 25): List<DocSearchResult> = findAnySignatures(query, limit, DocType.METHOD)
    suspend fun findAnyFieldSignatures(query: String? = null, limit: Int = 25): List<DocSearchResult> = findAnySignatures(query, limit, DocType.FIELD)

    suspend fun findSignaturesIn(className: String, query: String? = null, vararg docTypes: DocType, limit: Int = 25): List<DocSearchResult>
    suspend fun findMethodSignaturesIn(className: String, query: String? = null, limit: Int = 25) =
        findSignaturesIn(className, query, DocType.METHOD, limit = limit)
    suspend fun findFieldSignaturesIn(className: String, query: String? = null, limit: Int = 25) =
        findSignaturesIn(className, query, DocType.FIELD, limit = limit)
    suspend fun findMethodAndFieldSignaturesIn(className: String, query: String? = null, limit: Int = 25) =
        findSignaturesIn(className, query, DocType.METHOD, DocType.FIELD, limit = limit)

    suspend fun getClasses(query: String? = null, limit: Int = 25): List<String>
    suspend fun getClassesWithMethods(query: String? = null): List<String>
    suspend fun getClassesWithFields(query: String? = null): List<String>

    suspend fun resolveDoc(query: String): CachedDoc?
    suspend fun resolveDocAutocomplete(query: String): List<DocResolveResult>
}