package com.freya02.docs;

import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class HTMLElementList extends ArrayList<HTMLElement> {
	public HTMLElementList(@NotNull Collection<? extends HTMLElement> c) {
		super(c);
	}

	public HTMLElementList(@NotNull Elements elements) {
		super(elements.size());

		for (Element element : elements) {
			add(HTMLElement.wrap(element));
		}
	}

	@NotNull
	public static HTMLElementList fromElements(@NotNull Elements elements) {
		return new HTMLElementList(elements);
	}

	@NotNull
	public List<HTMLElement> getHtmlElements() {
		return this;
	}

	@NotNull
	public String toMarkdown(String delimiter) {
		return this.stream()
				.map(HTMLElement::getMarkdown)
				.collect(Collectors.joining(delimiter));
	}

	@NotNull
	public String toText() {
		return this.stream()
				.map(e -> e.getTargetElement().text())
				.collect(Collectors.joining());
	}
}
