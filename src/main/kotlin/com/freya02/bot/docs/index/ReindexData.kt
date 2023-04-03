package com.freya02.bot.docs.index

import com.freya02.bot.docs.metadata.parser.ImplementationMetadata
import com.freya02.docs.data.ClassDoc

data class ReindexData(val sourceUrl: String? = null) {
    fun getClassSourceUrlOrNull(classDoc: ClassDoc): String? {
        if (sourceUrl == null) return null

        val packageName = classDoc.packageName.replace('.', '/')
        val topLevelClassName = classDoc.className.substringBefore('.')
        return "$sourceUrl$packageName/$topLevelClassName.java"
    }

    fun getClassSourceUrl(classData: ImplementationMetadata.Class): String {
        if (sourceUrl == null)
            throw IllegalStateException("Method source URL could not be computed due to a missing source URL")

        val packageName = classData.packageName.replace('.', '/')
        val topLevelClassName = classData.topLevelName
        return "$sourceUrl$packageName/$topLevelClassName.java"
    }

    fun getMethodSourceUrl(methodData: ImplementationMetadata.Method): String {
        if (sourceUrl == null)
            throw IllegalStateException("Method source URL could not be computed due to a missing source URL")

        val methodRange = methodData.range
        return "${getClassSourceUrl(methodData.owner)}#L${methodRange.first}-L${methodRange.last}"
    }
}
