package com.freya02.bot.docs.index

import com.freya02.bot.docs.metadata.ImplementationMetadata
import com.freya02.docs.data.ClassDoc

data class ReindexData(val sourceUrl: String? = null) {
    fun getClassSourceUrl(classDoc: ClassDoc): String? {
        if (sourceUrl == null) return null

        val packageName = classDoc.packageName.replace('.', '/')
        val topLevelClassName = classDoc.className.substringBefore('.')
        return "$sourceUrl$packageName/$topLevelClassName.java"
    }

    fun getClassSourceUrl(classData: ImplementationMetadata.Class): String? {
        if (sourceUrl == null) return null

        val segments = classData.qualifiedName.split('.')
        val classIndex = segments.indexOfLast { it.all { c -> c.isLowerCase() || !c.isLetter() } } + 1
        val packageName = segments.take(classIndex).joinToString("/")
        //Only keep top level name, i.e. Message in Message.Attachment
        val topLevelClassName = segments[classIndex]
        return "$sourceUrl$packageName/$topLevelClassName.java"
    }
}
