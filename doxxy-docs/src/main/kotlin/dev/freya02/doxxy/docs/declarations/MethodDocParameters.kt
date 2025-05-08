package dev.freya02.doxxy.docs.declarations

@ConsistentCopyVisibility
data class MethodDocParameters internal constructor(val asString: String) {
    val parameters: List<MethodDocParameter> = asString
        .drop(1)
        .dropLast(1)
        .split(", ")
        .map { parameter ->
            try {
                MethodDocParameter.parse(parameter)
            } catch (e: Exception) {
                throw RuntimeException("Unable to parse parameter '$parameter'", e)
            }
        }
}