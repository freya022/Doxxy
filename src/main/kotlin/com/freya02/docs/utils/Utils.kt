package com.freya02.docs.utils

import com.freya02.docs.DocParseException
import com.freya02.docs.DocUtils.isJavadocVersionCorrect
import org.jsoup.nodes.Document

@Suppress("NOTHING_TO_INLINE")
inline fun requireDoc(boolean: Boolean) {
    if (!boolean)
        throw DocParseException()
}

fun Document.checkJavadocVersion() {
    if (!isJavadocVersionCorrect()) {
        throw DocParseException("Javadoc at '${baseUri()}' is not javadoc 17")
    }
}