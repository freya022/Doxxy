package com.freya02.bot.docs

@JvmInline
value class DocResolveChain(private val chain: List<String>) {
    val lastQualifiedSignature: String
        get() = chain.last()

    val lastSignature: String
        get() = lastQualifiedSignature.substringAfterLast('#')

    val secondLastQualifiedSignatureOrNull: String?
        get() = chain.dropLast(1).lastOrNull()

    override fun toString() = when (chain.size) {
        1 -> chain.first()
        else -> chain.first() + "#" + chain.drop(1).joinToString("#") { it.substringAfter('#') }
    }
}