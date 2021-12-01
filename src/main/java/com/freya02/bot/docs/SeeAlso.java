package com.freya02.bot.docs;

import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

public class SeeAlso {
	private final List<SeeAlsoItem> items = new ArrayList<>();

	public SeeAlso(Element detailValueElement) {
		for (Element a : detailValueElement.select("a")) {
			items.add(new SeeAlsoItem(a.text(), a.absUrl("href")));
		}
	}

	public List<SeeAlsoItem> getItems() {
		return items;
	}

	public static record SeeAlsoItem(String label, String link) {}
}
