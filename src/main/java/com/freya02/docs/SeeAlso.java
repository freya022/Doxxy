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

			//TODO maybe add another field to put resolved full methods (and others) signatures
			// Will be useful for the see also selection menu
			// Will have to analyse the href target and find out whether it's just a class, a method or a field
			// So, put a target type and the target full signature
			references.add(new SeeAlsoReference(textBuilder.toString(), seeAlsoClassElement.absUrl("href")));
		}
	}

	public List<SeeAlsoReference> getReferences() {
		return references;
	}
}
