package com.freya02.bot.docs.index

import com.freya02.bot.docs.cached.CachedClass
import com.freya02.bot.docs.cached.CachedDoc
import com.freya02.bot.docs.cached.CachedField
import com.freya02.bot.docs.cached.CachedMethod

interface IDocIndex {
    fun getClassDoc(className: String): CachedClass?

    fun getMethodDoc(className: String, identifier: String): CachedMethod?

    fun getMethodDoc(fullSignature: String): CachedMethod? {
        val split = fullSignature.split("#")
        return when {
            split.size != 2 -> null
            else -> getMethodDoc(split[0], split[1])
        }
    }

    fun getFieldDoc(className: String, fieldName: String): CachedField?

    fun getFieldDoc(fullSignature: String): CachedField? {
        val split = fullSignature.split("#")
        return when {
            split.size != 2 -> null
            else -> getFieldDoc(split[0], split[1])
        }
    }

    fun findAnySignatures(docType: DocType, query: String?): List<DocSearchResult>
    fun findAnyMethodSignatures(query: String? = null): List<DocSearchResult> = findAnySignatures(DocType.METHOD, query)
    fun findAnyFieldSignatures(query: String? = null): List<DocSearchResult> = findAnySignatures(DocType.FIELD, query)

    fun findSignaturesIn(className: String, query: String? = null, vararg docTypes: DocType): List<DocSearchResult>
    fun findMethodSignaturesIn(className: String, query: String? = null) = findSignaturesIn(className, query, DocType.METHOD)
    fun findFieldSignaturesIn(className: String, query: String? = null) = findSignaturesIn(className, query, DocType.FIELD)
    fun findMethodAndFieldSignaturesIn(className: String, query: String? = null) =
        findSignaturesIn(className, query, DocType.METHOD, DocType.FIELD)

    fun getClasses(query: String? = null): List<String>
    fun getClassesWithMethods(query: String? = null): List<String>
    fun getClassesWithFields(query: String? = null): List<String>

    fun resolveDoc(query: String): CachedDoc?
    fun resolveDocAutocomplete(query: String): List<DocResolveData>
}