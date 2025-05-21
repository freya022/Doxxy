package dev.freya02.doxxy.bot.docs.metadata.data

import dev.freya02.doxxy.bot.docs.metadata.parser.FieldMetadataMap
import dev.freya02.doxxy.bot.docs.metadata.parser.MethodMetadataMap

class ClassMetadata(val name: String, val range: IntRange, val enclosedBy: String?) {
    val methodMetadataMap: MethodMetadataMap = hashMapOf()
    val fieldMetadataMap: FieldMetadataMap = hashMapOf()
    val extends: MutableList<String> = arrayListOf()
    val implements: MutableList<String> = arrayListOf()
}