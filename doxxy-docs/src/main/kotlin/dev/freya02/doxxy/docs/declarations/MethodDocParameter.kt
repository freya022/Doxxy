package dev.freya02.doxxy.docs.declarations


@ConsistentCopyVisibility
data class MethodDocParameter internal constructor(
    val annotations: Set<String>,
    val type: String,
    val simpleType: String,
    val name: String
)
