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

    fun getAllMethodSignatures(): Collection<String>

    fun getMethodSignatures(className: String): Collection<String>

    fun getAllFieldSignatures(): Collection<String>

    fun getFieldSignatures(className: String): Collection<String>

    fun getMethodAndFieldSignatures(className: String): Collection<String>

    fun getSimpleNameList(): Collection<String>

    fun getClassesWithMethods(): Collection<String>

    fun getClassesWithFields(): Collection<String>
}