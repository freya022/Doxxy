package com.freya02.docs.data

enum class DocDetailType(val elementText: String) {
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
        fun parseType(elementText: String): DocDetailType? = values().find { it.elementText == elementText }
    }
}