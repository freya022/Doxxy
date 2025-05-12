package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.exceptions.DocParseException
import org.jsoup.nodes.Element

class JavadocElement private constructor(val targetElement: Element) {

    override fun toString(): String {
        return targetElement.wholeText()
    }

    internal companion object {

        internal fun wrap(targetElement: Element?): JavadocElement =
            targetElement?.let { JavadocElement(it) } ?: throw DocParseException()

        internal fun tryWrap(targetElement: Element?): JavadocElement? =
            targetElement?.let { JavadocElement(it) }
    }
}