package com.freya02.docs.data

data class MethodDocParameter(val annotations: Set<String>, val type: String, val name: String) {
    val simpleType: String by lazy { type.substringAfterLast('.') }
}
