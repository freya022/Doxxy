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
			final String text = seeAlsoClassElement.text();

			final StringBuilder textBuilder = new StringBuilder(text);
			final int index = text.lastIndexOf('.');

			if (index > -1) {
				textBuilder.replace(index, index + 1, "#");
			}

			//TODO maybe add another field to put resolved full methods signatures
			// Will be useful for the see also selection menu
			references.add(new SeeAlsoReference(textBuilder.toString(), seeAlsoClassElement.absUrl("href")));
		}
	}

	public List<SeeAlsoReference> getReferences() {
		return references;
	}
}
