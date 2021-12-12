package com.freya02.docs;

import com.freya02.bot.utils.HTMLElement;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

public class SeeAlso extends HTMLElement {
	public static record SeeAlsoReference(String text, String link) {}

	private final List<SeeAlsoReference> references = new ArrayList<>();

	public SeeAlso(HTMLElement element) {
		super(element.getTargetElement());

		for (Element seeAlsoClassElement : element.getTargetElement().select("dd > a")) {
			references.add(new SeeAlsoReference(seeAlsoClassElement.text(), seeAlsoClassElement.absUrl("href")));
		}
	}

	public List<SeeAlsoReference> getReferences() {
		return references;
	}
}
