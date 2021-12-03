package com.freya02.docs;

public enum InheritedType {
	METHOD("method"),
	FIELD("field");

	private final String classSuffix;

	InheritedType(String classSuffix) {
		this.classSuffix = classSuffix;
	}

	public String getClassSuffix() {
		return classSuffix;
	}
}
