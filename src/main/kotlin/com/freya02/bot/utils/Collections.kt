package com.freya02.bot.utils

private fun <T> Iterable<T>.joinTo(buffer: StringBuilder, separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", limit: Int = -1, truncated: CharSequence = "...", transform: ((T) -> CharSequence)? = null): StringBuilder {
    buffer.append(prefix)
    for ((count, element) in this.withIndex()) {
        val elementStr = when {
            transform != null -> transform(element)
            else -> element.toString()
        }
        if (limit < 0 || buffer.length + elementStr.length + truncated.length + postfix.length <= limit) {
            if (count > 0) buffer.append(separator)
            buffer.append(elementStr)
        } else {
            buffer.append(truncated)
            break
        }
    }
    buffer.append(postfix)
    return buffer
}

fun <T> Iterable<T>.joinLengthyString(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = "", lengthLimit: Int = -1, truncated: CharSequence = "...", transform: ((T) -> CharSequence)? = null): String {
    return joinTo(StringBuilder(), separator, prefix, postfix, lengthLimit, truncated, transform).toString()
}