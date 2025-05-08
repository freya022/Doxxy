package dev.freya02.doxxy.docs.sections

import dev.freya02.doxxy.docs.HTMLElement
import dev.freya02.doxxy.docs.HTMLElementList

class DocDetail internal constructor(detailType: Type, htmlElements: List<HTMLElement>) : HTMLElementList(htmlElements) {
    val detailString: String = detailType.elementText

    enum class Type(val elementText: String) {
        PARAMETERS("Parameters:"),
        TYPE_PARAMETERS("Type Parameters:"),
        RETURNS("Returns:"),
        SEE_ALSO("See Also:"),
        SPECIFIED_BY("Specified by:"),
        SINCE("Since:"),
        OVERRIDES("Overrides:"),
        INCUBATING("Incubating:"),
        ALL_KNOWN_IMPLEMENTING_CLASSES("All Known Implementing Classes:"),
        ALL_IMPLEMENTED_INTERFACES("All Implemented Interfaces:"),
        ALL_KNOWN_SUBINTERFACES("All Known Subinterfaces:"),
        DIRECT_KNOWN_SUBCLASSES("Direct Known Subclasses:"),
        DEFAULT("Default:"),
        SUPER_INTERFACES("All Superinterfaces:"),
        FUNCTIONAL_INTERFACE("Functional Interface:"),
        ENCLOSING_CLASS("Enclosing class:"),
        AUTHOR("Author:"),
        ENCLOSING_INTERFACE("Enclosing interface:"),
        THROWS("Throws:");

        companion object {
            fun parseType(elementText: String): Type? = entries.find { it.elementText == elementText }
        }
    }
}