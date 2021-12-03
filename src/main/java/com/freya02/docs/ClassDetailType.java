package com.freya02.docs;

public enum ClassDetailType {
	FIELD("field.detail"),
	CONSTRUCTOR("constructor.detail"),
	METHOD("method.detail");

	private final String detailId;
	ClassDetailType(String detailId) {
		this.detailId = detailId;
	}

	public String getDetailId() {
		return detailId;
	}
}
