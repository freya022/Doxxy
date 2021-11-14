package com.freya02.bot.docs;

import com.freya02.bot.utils.HTMLElement;
import com.freya02.bot.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BasicDocs {
	private static final Pattern DETAIL_PATTERN = Pattern.compile("\\s*<dt>(\\X*?)</dt>\\s*?\n" +
			"\\s*<dd>(\\X*?)</dd>");

	protected final Document document;

	private final HTMLElement description;
	private final HTMLElement classDecl;
	private final List<Detail> details;

	protected BasicDocs(Document document, HTMLElement description, HTMLElement classDecl, List<Detail> details) {
		this.document = document;

		this.description = description;
		this.classDecl = classDecl;
		this.details = details;
	}

	public static BasicDocs of(@NotNull String url) throws IOException {
		try {
			final Document document = Utils.getDocument(url);

			final Elements descriptionElements = document.selectXpath("/html/body/main/div[@class='contentContainer']/div[@class='description']/ul/li/div[@class='block']");
			final HTMLElement description = descriptionElements.isEmpty() ? new HTMLElement(new Element("div")) : new HTMLElement(descriptionElements.get(0));

			final Elements classDeclElements = document.selectXpath("/html/body/main/div[@class='header']/h2");
			final HTMLElement classDecl = new HTMLElement(classDeclElements.get(0));

			final List<Detail> details = new ArrayList<>();

			final Elements detailsElements = document.selectXpath("/html/body/main/div[@class='contentContainer']/div[@class='description']/ul/li/dl");
			parseDetails(details, detailsElements);

			if (classDecl.getTargetElement().text().startsWith("Enum")) {
				return new EnumDocs(document, description, classDecl, details);
			} else {
				return new ClassDocs(document, description, classDecl, details);
			}
		} catch (Exception e) {
			throw new IOException("Unable to read docs for " + url, e);
		}
	}

	private static void parseDetails(List<Detail> details, Elements detailsElements) {
		for (Element element : detailsElements) {
			final String html = element.html();

			final Matcher matcher = DETAIL_PATTERN.matcher(html);
			while (matcher.find()) {
				details.add(new Detail(
						new HTMLElement(Jsoup.parseBodyFragment(matcher.group(1), element.baseUri()).selectFirst("body")),
						new HTMLElement(Jsoup.parseBodyFragment(matcher.group(2), element.baseUri()).selectFirst("body"))
				));
			}
		}
	}

	public HTMLElement getDescription() {
		return description;
	}

	public HTMLElement getClassDecl() {
		return classDecl;
	}

	public List<Detail> getDetails() {
		return details;
	}

	public EmbedBuilder toEmbed() {
		final EmbedBuilder builder = new EmbedBuilder();

		builder.setTitle(getClassDecl().getMarkdown(), document.baseUri());
		builder.setDescription(getDescription().getMarkdown());
		for (Detail detail : getDetails()) {
			builder.addField(detail.key().getMarkdown(), detail.value().getMarkdown2(false), false);
		}

		return builder;
	}
}
