package com.freya02.bot.docs.metadata

class ClassMetadata(val name: String, val enclosedBy: String?) {
    val resolvedMap: ResolvedClassesList = hashMapOf()
    val methodMetadataMap: MethodMetadataMap = hashMapOf()
    val extends: MutableList<String> = arrayListOf()
    val implements: MutableList<String> = arrayListOf()
}