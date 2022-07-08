package com.freya02.docs.data

enum class TargetType {
    CLASS, METHOD, FIELD, UNKNOWN;

    companion object {
        @JvmStatic
        fun fromFragment(fragment: String?): TargetType {
            return when {
                fragment == null -> CLASS
                fragment.contains("(") -> METHOD
                else -> FIELD
            }
        }
    }
}