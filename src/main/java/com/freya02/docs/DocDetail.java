package com.freya02.docs;

import com.freya02.bot.utils.HTMLElement;

import java.util.List;
import java.util.stream.Collectors;

public class DocDetail {
	private final DocDetailType detailType;
	private final List<HTMLElement> htmlElements;

	public DocDetail(DocDetailType detailType, List<HTMLElement> htmlElements) {
		this.detailType = detailType;
		this.htmlElements = htmlElements;
	}

	public List<HTMLElement> getHtmlElements() {
		return htmlElements;
	}

	public DocDetailType getDetailType() {
		return detailType;
	}

	public String getDetailString() {
		return detailType.getElementText();
	}

	public String toMarkdown() {
		return htmlElements.stream()
				.map(HTMLElement::getMarkdown)
				.collect(Collectors.joining("\n"));
	}
}
