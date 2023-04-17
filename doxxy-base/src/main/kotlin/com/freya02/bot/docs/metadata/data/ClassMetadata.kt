package com.freya02.bot.docs.metadata.data

import com.freya02.bot.docs.metadata.parser.FieldMetadataMap
import com.freya02.bot.docs.metadata.parser.MethodMetadataMap
import com.freya02.bot.docs.metadata.parser.ResolvedClassesList

class ClassMetadata(val name: String, val enclosedBy: String?) {
    val resolvedMap: ResolvedClassesList = hashMapOf()
    val methodMetadataMap: MethodMetadataMap = hashMapOf()
    val fieldMetadataMap: FieldMetadataMap = hashMapOf()
    val extends: MutableList<String> = arrayListOf()
    val implements: MutableList<String> = arrayListOf()
}