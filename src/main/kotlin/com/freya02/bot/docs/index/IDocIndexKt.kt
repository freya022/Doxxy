package com.freya02.bot.docs.index

import com.freya02.bot.docs.cached.CachedField
import com.freya02.bot.docs.cached.CachedMethod
import java.io.IOException

interface IDocIndexKt {
    @Throws(IOException::class)
    fun getMethodDoc(className: String, identifier: String): CachedMethod?

    @Throws(IOException::class)
    fun getMethodDoc(fullSignature: String): CachedMethod? {
        val split = fullSignature.split("#")
        return when {
            split.size != 2 -> null
            else -> getMethodDoc(split[0], split[1])
        }
    }

    @Throws(IOException::class)
    fun getFieldDoc(className: String, fieldName: String): CachedField?

    fun getFieldDoc(fullSignature: String): CachedField? {
        val split = fullSignature.split("#")
        return when {
            split.size != 2 -> null
            else -> getFieldDoc(split[0], split[1])
        }
    }

    fun findAnyMethodSignatures(query: String? = null): Collection<String>

    fun findMethodSignatures(className: String, query: String? = null): Collection<String>

    fun findAnyFieldSignatures(query: String? = null): Collection<String>

    fun findFieldSignatures(className: String, query: String? = null): Collection<String>

    fun findMethodAndFieldSignatures(className: String, query: String? = null): Collection<String>

    fun getClasses(): Collection<String>

    fun getClassesWithMethods(): Collection<String>

    fun getClassesWithFields(): Collection<String>
}