package com.freya02.bot.docs.index

import com.freya02.docs.data.ClassDoc
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration
import io.github.classgraph.ClassInfo
import java.nio.file.Path

data class ReindexData(val sourceUrl: String? = null, val binaryPaths: List<Path> = emptyList()) {
    fun getClassSourceUrl(classDoc: ClassDoc): String? {
        if (sourceUrl == null) return null

        val packageName = classDoc.packageName.replace('.', '/')
        val topLevelClassName = classDoc.className.substringBefore('.')
        return "$sourceUrl$packageName/$topLevelClassName.java"
    }

    fun getClassSourceUrl(typeDeclaration: ResolvedReferenceTypeDeclaration): String? {
        if (sourceUrl == null) return null

        val packageName = typeDeclaration.packageName.replace('.', '/')
        val topLevelClassName = typeDeclaration.className.substringBefore('.')
        return "$sourceUrl$packageName/$topLevelClassName.java"
    }
}
