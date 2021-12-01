package com.freya02.bot.docs;

import com.freya02.bot.utils.Utils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class ClassList {
	public static void runGeneration(String url) throws IOException {
		final Document document = Utils.getDocument(url);

		final Elements elements = document.selectXpath("/html/body/main/ul/li/a");

		for (Element element : elements) {
			ClassReferences.put(element.attr("title").split(" ")[2] + '.' + element.text(), element.absUrl("href"));
		}
	}
}
