package com.freya02.bot.docs;

import com.freya02.bot.utils.HTMLElement;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class MethodDocs {
	private final HTMLElement signature;
	private final HTMLElement description;
	private final List<Detail> details;

	private MethodDocs(HTMLElement signature, HTMLElement description, List<Detail> details) {
		this.signature = signature;
		this.description = description;
		this.details = details;
	}

	public static List<MethodDocs> of(Document classDocument) {
		final List<MethodDocs> methodDocs = new ArrayList<>();

		final Elements elements = classDocument.selectXpath("/html/body/main/div[@class='contentContainer']/div[@class='details']/ul/li/section[@role='region']/ul/li/ul");
		for (Element element : elements) {
			final Element signatureElement = element.selectFirst("pre");
			if (signatureElement == null) {
				System.err.println("Got a null signature at " + element.text());

				continue;
			}

			final HTMLElement signature = new HTMLElement(signatureElement);

			final Element descriptionElement = element.selectFirst("div");
			if (descriptionElement == null) continue;

			final HTMLElement description = new HTMLElement(descriptionElement);

			final List<Detail> details = new ArrayList<>();

			//Inherit this from Docs if possible
//			BasicDocs.parseDetails(details, element.select("dl"));

			methodDocs.add(new MethodDocs(signature, description, details));
		}

		return methodDocs;
	}
}
