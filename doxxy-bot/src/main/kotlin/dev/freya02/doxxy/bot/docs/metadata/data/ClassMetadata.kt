package dev.freya02.doxxy.bot.docs.metadata.data

import dev.freya02.doxxy.bot.docs.metadata.parser.FieldMetadataMap
import dev.freya02.doxxy.bot.docs.metadata.parser.MethodMetadataMap
import dev.freya02.doxxy.bot.docs.metadata.parser.ResolvedClassesList

class ClassMetadata(val name: String, val range: IntRange, val enclosedBy: String?) {
    val resolvedMap: ResolvedClassesList = hashMapOf()
    val methodMetadataMap: MethodMetadataMap = hashMapOf()
    val fieldMetadataMap: FieldMetadataMap = hashMapOf()
    val extends: MutableList<String> = arrayListOf()
    val implements: MutableList<String> = arrayListOf()
}