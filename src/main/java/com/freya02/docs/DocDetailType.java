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
	THROWS("Throws:");

	private final String elementText;
	DocDetailType(String elementText) {
		this.elementText = elementText;
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
