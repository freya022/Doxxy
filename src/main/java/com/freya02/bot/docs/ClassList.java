package com.freya02.bot.docs;

import com.freya02.bot.utils.Utils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ClassList {
	private final Map<String, String> classToUrlMap = new HashMap<>();

	private ClassList(Document document) {
		final Elements elements = document.selectXpath("/html/body/main/ul/li/a");

		for (Element element : elements) {
			classToUrlMap.put(element.text(), element.attributes().get("href"));
		}
	}

	public static ClassList of(String url) throws IOException {
		final Document document = Utils.getDocument(url);

		return new ClassList(document);
	}

	public Map<String, String> getClassToUrlMap() {
		return classToUrlMap;
	}
}
