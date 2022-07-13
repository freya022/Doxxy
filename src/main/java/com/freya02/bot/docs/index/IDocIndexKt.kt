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

    fun getMethodDocSuggestions(className: String): Collection<String>

    fun getMethodDocSuggestions(): Collection<String>

    fun getFieldDocSuggestions(className: String): Collection<String>

    fun getMethodAndFieldDocSuggestions(className: String): Collection<String>

    fun getFieldDocSuggestions(): Collection<String>

    fun getSimpleNameList(): Collection<String>

    fun getClassesWithMethods(): Collection<String>

    fun getClassesWithFields(): Collection<String>
}