package com.freya02.docs;

import org.jetbrains.annotations.Nullable;

public enum DocDetailType {
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

	private final String elementText;
	DocDetailType(String elementText) {
		this.elementText = elementText;
	}

	public String getElementText() {
		return elementText;
	}

	@Nullable
	public static DocDetailType parseType(String elementText) {
		for (DocDetailType type : values()) {
			if (type.elementText.equals(elementText)) {
				return type;
			}
		}

		return null;
	}
}
