package com.freya02.bot.docs;

import com.freya02.bot.utils.HTMLElement;
import com.freya02.bot.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BasicDocs {
	private static final Pattern DETAIL_PATTERN = Pattern.compile("\\s*<dt>(\\X*?)</dt>\\s*?\n" +
			"\\s*<dd>(\\X*?)</dd>");
	private static final List<String> allowedDetails = List.of("Since");

	protected final Document document;
	private final String url;

	private final HTMLElement description;
	private final HTMLElement classDecl;
	private final List<Detail> details = new ArrayList<>();
	private SeeAlso seeAlso = null;

	private final List<MethodDocs> methodDocs;

	public BasicDocs(String url) throws IOException {
		this.url = url;
		this.document = Utils.getDocument(url);

		final Elements classDeclElements = document.selectXpath("/html/body/main/div[@class='header']/h2");
		this.classDecl = new HTMLElement(classDeclElements.get(0));

		final Elements descriptionElements = document.selectXpath("/html/body/main/div[@class='contentContainer']/div[@class='description']/ul/li/div[@class='block']");
		this.description = descriptionElements.isEmpty() ? new HTMLElement(new Element("div")) : new HTMLElement(descriptionElements.get(0));

		final Elements detailsElements = document.selectXpath("/html/body/main/div[@class='contentContainer']/div[@class='description']/ul/li/dl");
		parseDetails(detailsElements);

		this.methodDocs = MethodDocs.of(document);
	}

	protected void parseDetails(Elements detailsElements) {
		for (Element element : detailsElements) {
			final String html = element.html();

			final Matcher matcher = DETAIL_PATTERN.matcher(html);
			while (matcher.find()) {
				final Element detailNameElement = Jsoup.parseBodyFragment(matcher.group(1), element.baseUri()).selectFirst("body");
				if (detailNameElement == null) throw new IllegalArgumentException("No detail name body in body fragment ??");

				final Element detailValueElement = Jsoup.parseBodyFragment(matcher.group(2), element.baseUri()).selectFirst("body");
				if (detailValueElement == null) throw new IllegalArgumentException("No detail value body in body fragment ??");

				final String detailName = detailNameElement.text();
				if (detailName.equals("See Also:")) {
					seeAlso = new SeeAlso(detailValueElement);
				} else if (allowedDetails.contains(detailName.substring(0, detailName.length() - 1))) {
					details.add(new Detail(
							new HTMLElement(detailNameElement),
							new HTMLElement(detailValueElement)
					));
				}
			}
		}
	}

	public HTMLElement getDescription() {
		return description;
	}

	public HTMLElement getClassDecl() {
		return classDecl;
	}

	public SeeAlso getSeeAlso() {
		return seeAlso;
	}

	public List<Detail> getDetails() {
		return details;
	}

	public EmbedBuilder toEmbed() {
		final EmbedBuilder builder = new EmbedBuilder();

		builder.setTitle(getClassDecl().getMarkdown(), document.baseUri());

		final String description = getDescription().getMarkdown();
		if (description.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH) {
			builder.setDescription("Description too long, please see [here](" + url + ")");
		} else {
			builder.setDescription(description);
		}

		for (Detail detail : getDetails()) {
			builder.addField(detail.key().getMarkdown(), detail.value().getMarkdown2(false), false);
		}

		return builder;
	}
}
