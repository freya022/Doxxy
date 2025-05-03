package dev.freya02.doxxy.docs.utils

import dev.freya02.doxxy.docs.DocParseException
import dev.freya02.doxxy.docs.DocUtils.isJavadocVersionCorrect
import org.jsoup.nodes.Document

@Suppress("NOTHING_TO_INLINE")
internal inline fun requireDoc(boolean: Boolean) {
    if (!boolean)
        throw DocParseException()
}

internal fun Document.checkJavadocVersion() {
    if (!isJavadocVersionCorrect()) {
        throw DocParseException("Javadoc at '${baseUri()}' is not javadoc 17")
    }
}