package com.freya02.docs.data;

import com.freya02.docs.HTMLElement;
import com.freya02.docs.HTMLElementList;

import java.util.List;

public class DocDetail extends HTMLElementList {
	private final DocDetailType detailType;

	public DocDetail(DocDetailType detailType, List<HTMLElement> htmlElements) {
		super(htmlElements);

		this.detailType = detailType;
	}

	public DocDetailType getDetailType() {
		return detailType;
	}

	public String getDetailString() {
		return detailType.getElementText();
	}
}
