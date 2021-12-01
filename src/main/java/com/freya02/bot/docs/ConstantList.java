package com.freya02.bot.docs;

import com.freya02.bot.utils.Utils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class ConstantList {
	private final ClassMap<ConstantMap> classToConstantMap = new ClassMap<>();

	public ConstantList(Document document) {
		final Elements blockGroupElements = document.selectXpath("/html/body/main/div[@class='constantValuesContainer']/section/ul[@class='blockList']/li/table");

		for (Element element : blockGroupElements) {
			final Element classNameElement = element.selectFirst("caption");
			if (classNameElement == null) {
				throw new IllegalArgumentException("No class name for constants at: " + element);
			}

			final Elements lines = element.selectXpath("//tbody/tr");

			final Element classLinkElement = classNameElement.selectFirst("a");
			if (classLinkElement == null) throw new IllegalArgumentException("No class link found");

			final ClassReference name = ClassReferences.computeIfAbsent(classNameElement.text(), classLinkElement.absUrl("href"));
			final ConstantMap constantMap = new ConstantMap();

			if (!lines.isEmpty()) {
				lines.remove(0);

				for (Element line : lines) {
					final Element fieldModifierTypeElement = line.selectFirst("td[class=colFirst] code");
					if (fieldModifierTypeElement == null) throw new IllegalArgumentException("No modifier & type");

					final Element fieldNameElement = line.selectFirst("th[class=colSecond]");
					if (fieldNameElement == null) throw new IllegalArgumentException("No field name");

					final Element fieldValueElement = line.selectFirst("td[class=colLast]");
					if (fieldValueElement == null) throw new IllegalArgumentException("No field value");

					final ClassReference fieldType;
					final Element classLink = fieldModifierTypeElement.selectFirst("a[href]");
					if (classLink != null) {
						fieldType = ClassReferences.computeIfAbsent(classLink.text(), classLink.absUrl("href"));
					} else {
						fieldType = ClassReferences.computeIfAbsent(fieldModifierTypeElement.text().substring("public static final ".length()));
					}

					constantMap.put(fieldNameElement.text(), new ConstantMap.Constant(fieldType, fieldValueElement.text()));
				}
			}

			classToConstantMap.put(name, constantMap);
		}
	}

	public ClassMap<ConstantMap> getClassToConstantMap() {
		return classToConstantMap;
	}

	public static ConstantList of(String constantsUrl) throws IOException {
		final Document document = Utils.getDocument(constantsUrl);

		return new ConstantList(document);
	}
}
