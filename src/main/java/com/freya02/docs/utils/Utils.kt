package com.freya02.docs.utils

import com.freya02.docs.DocParseException

@Suppress("NOTHING_TO_INLINE")
inline fun requireDoc(boolean: Boolean) {
    if (!boolean)
        throw DocParseException()
}