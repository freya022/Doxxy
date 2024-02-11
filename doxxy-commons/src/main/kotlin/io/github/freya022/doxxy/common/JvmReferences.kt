package io.github.freya022.doxxy.common

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class SimpleClassName(val str: String) {
    init {
        if ('#' in str || '.' in str) {
            throw IllegalArgumentException("Invalid simple class name: $str")
        }
    }

    override fun toString(): String = str
}

@JvmInline
@Serializable
value class PartialIdentifier(val str: String) {
    val type get() = when {
        '(' in str -> Type.FULL_METHOD
        str.all { !it.isLetter() || it.isUpperCase() } -> Type.FIELD
        else -> Type.OVERLOADS
    }

    enum class Type {
        FULL_METHOD,
        OVERLOADS,
        FIELD
    }

    init {
        if ('#' in str) {
            throw IllegalArgumentException("Invalid partial identifier: $str")
        }
    }

    override fun toString(): String = str
}

@JvmInline
@Serializable
value class QualifiedPartialIdentifier(val str: String) {
    val className get() = SimpleClassName(str.substringBefore('#'))
    val identifier get() = PartialIdentifier(str.substringAfter('#'))

    init {
        if ('#' !in str) {
            throw IllegalArgumentException("Invalid qualified partial identifier: $str")
        }
    }

    override fun toString(): String = str
}