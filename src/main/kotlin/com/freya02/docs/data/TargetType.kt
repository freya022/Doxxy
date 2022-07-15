package com.freya02.docs.data

enum class TargetType(val id: Int) {
    CLASS(1),
    METHOD(2),
    FIELD(3),
    UNKNOWN(-1);

    companion object {
        @JvmStatic
        fun fromFragment(fragment: String?): TargetType {
            return when {
                fragment == null -> CLASS
                fragment.contains("(") -> METHOD
                else -> FIELD
            }
        }

        fun fromId(type: Int): TargetType {
            return values().find { it.id == type } ?: throw IllegalArgumentException("Unknown type: $type")
        }
    }
}