package com.freya02.bot.docs.index

import com.freya02.docs.data.ClassDoc

data class ReindexData(val sourceUrl: String? = null) {
    fun getClassSourceUrl(classDoc: ClassDoc): String? {
        if (sourceUrl == null) return null

        val packageName = classDoc.packageName.replace('.', '/')
        val topLevelClassName = classDoc.className.substringBefore('.')
        return "$sourceUrl$packageName/$topLevelClassName.java"
    }
}
