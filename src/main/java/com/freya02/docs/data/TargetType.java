package com.freya02.docs.data;

import org.jetbrains.annotations.Nullable;

public enum TargetType {
	CLASS,
	METHOD,
	FIELD,
	UNKNOWN;

	public static TargetType fromFragment(@Nullable String fragment) {
		if (fragment == null) {
			return CLASS;
		} else if (fragment.contains("(")) {
			return METHOD;
		} else {
			return FIELD;
		}
	}
}
