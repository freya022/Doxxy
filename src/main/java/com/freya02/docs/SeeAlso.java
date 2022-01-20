package com.freya02.docs;

import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

public class SeeAlso {
	public record SeeAlsoReference(String text, String link) {}

	private final List<SeeAlsoReference> references = new ArrayList<>();

	public SeeAlso(@NotNull DocDetail docDetail) {
		for (Element seeAlsoClassElement : docDetail.getHtmlElements().get(0).getTargetElement().select("dd > ul > li > a")) {
			references.add(new SeeAlsoReference(seeAlsoClassElement.text(), seeAlsoClassElement.absUrl("href")));
		}
	}

	public List<SeeAlsoReference> getReferences() {
		return references;
	}
}
