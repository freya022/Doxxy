package com.freya02.docs;

public enum ClassDetailType {
	FIELD("Field Detail"),
	CONSTRUCTOR("Constructor Detail"),
	METHOD("Method Detail");

	private final String detailTitleName;
	ClassDetailType(String detailTitleName) {

		this.detailTitleName = detailTitleName;
	}

	public String getDetailTitleName() {
		return detailTitleName;
	}
}
