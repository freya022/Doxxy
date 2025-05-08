package dev.freya02.doxxy.bot.docs.index

import dev.freya02.doxxy.bot.docs.metadata.parser.ImplementationMetadata
import dev.freya02.doxxy.docs.declarations.JavadocClass

data class ReindexData(val sourceUrl: String? = null) {
    fun getClassSourceUrlOrNull(clazz: JavadocClass): String? {
        if (sourceUrl == null) return null

        val packageName = clazz.packageName.replace('.', '/')
        val topLevelClassName = clazz.className.substringBefore('.')
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
