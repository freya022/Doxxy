package com.freya02.docs.data;

public enum ClassDetailType {
	FIELD("field-detail"),
	CONSTRUCTOR("constructor-detail"),
	METHOD("method-detail"),
	ANNOTATION_ELEMENT("annotation-interface-element-detail");

	private final String detailId;
	ClassDetailType(String detailId) {
		this.detailId = detailId;
	}

	public String getDetailId() {
		return detailId;
	}
}
