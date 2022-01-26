package com.freya02.docs;

import com.freya02.botcommands.api.Logging;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class SeeAlso {
	private static final Logger LOGGER = Logging.getLogger();

	public record SeeAlsoReference(String text, String link, TargetType targetType, @Nullable String fullSignature) {}

	private final List<SeeAlsoReference> references = new ArrayList<>();

	public SeeAlso(@NotNull DocDetail docDetail) {
		for (Element seeAlsoClassElement : docDetail.getHtmlElements().get(0).getTargetElement().select("dd > ul > li > a")) {
			try {
				final String href = seeAlsoClassElement.absUrl("href");
				final DocSourceType sourceType = DocSourceType.fromUrl(href);
				if (sourceType == null) {
					references.add(new SeeAlsoReference(seeAlsoClassElement.text(), href, TargetType.UNKNOWN, null));

					continue;
				}

				final ClassDocs classDocs = ClassDocs.getSource(sourceType);
				//Class should be detectable as all URLs are pulled first
				if (classDocs.isValidURL(href)) {
					//Class exists

					//Is it a class, method, or field

					final JavadocUrl javadocUrl = JavadocUrl.fromURL(href);

					final String className = javadocUrl.getClassName();

					final String targetAsText = getTargetAsText(seeAlsoClassElement);
					final SeeAlsoReference ref = switch (javadocUrl.getTargetType()) {
						case CLASS -> new SeeAlsoReference(targetAsText, href, TargetType.CLASS, className);
						case METHOD -> new SeeAlsoReference(targetAsText, href, TargetType.METHOD, className + "#" + DocUtils.getSimpleSignature(javadocUrl.getFragment()));
						case FIELD -> new SeeAlsoReference(targetAsText, href, TargetType.FIELD, className + "#" + javadocUrl.getFragment());
						default -> throw new IllegalStateException("Unexpected javadoc target type: " + javadocUrl.getTargetType());
					};

					references.add(ref);
				} else {
					references.add(new SeeAlsoReference(seeAlsoClassElement.text(), href, TargetType.UNKNOWN, null));
				}
			} catch (Exception e) {
				LOGGER.error("An exception occurred while retrieving a 'See also' detail", e);
			}
		}
	}

	@NotNull
	private String getTargetAsText(Element seeAlsoClassElement) {
		final String text = seeAlsoClassElement.text();

		final StringBuilder textBuilder = new StringBuilder(text);
		final int index = text.lastIndexOf('.');

		if (index > -1) {
			textBuilder.replace(index, index + 1, "#");
		}

		return textBuilder.toString();
	}

	public List<SeeAlsoReference> getReferences() {
		return references;
	}
}
