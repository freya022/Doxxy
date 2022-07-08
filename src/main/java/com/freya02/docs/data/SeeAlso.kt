package com.freya02.docs.data;

import com.freya02.botcommands.api.Logging;
import com.freya02.docs.ClassDocs;
import com.freya02.docs.DocSourceType;
import com.freya02.docs.DocUtils;
import com.freya02.docs.JavadocUrl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SeeAlso {
	private static final Logger LOGGER = Logging.getLogger();

	public static final class SeeAlsoReference {
		private final String text;
		private final String link;
		private final TargetType targetType;
		private final @Nullable String fullSignature;

		public SeeAlsoReference(String text, String link, TargetType targetType, @Nullable String fullSignature) {
			this.text = text;
			this.link = link;
			this.targetType = targetType;
			this.fullSignature = fullSignature;
		}

		public String text() {return text;}

		public String link() {return link;}

		public TargetType targetType() {return targetType;}

		public @Nullable String fullSignature() {return fullSignature;}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			SeeAlsoReference that = (SeeAlsoReference) o;

			if (targetType != that.targetType) return false;
			return Objects.equals(fullSignature, that.fullSignature);
		}

		@Override
		public int hashCode() {
			int result = targetType.hashCode();
			result = 31 * result + (fullSignature != null ? fullSignature.hashCode() : 0);
			return result;
		}
	}

	private final List<SeeAlsoReference> references = new ArrayList<>();

	public SeeAlso(DocSourceType type, @NotNull DocDetail docDetail) {
		for (Element seeAlsoClassElement : docDetail.getHtmlElements().get(0).getTargetElement().select("dd > ul > li > a")) {
			try {
				final String href = type.toOnlineURL(seeAlsoClassElement.absUrl("href"));
				final DocSourceType sourceType = DocSourceType.fromUrl(href);
				if (sourceType == null) {
					tryAddReference(new SeeAlsoReference(seeAlsoClassElement.text(), href, TargetType.UNKNOWN, null));

					continue;
				}

				final ClassDocs classDocs = ClassDocs.getSource(sourceType);
				//Class should be detectable as all URLs are pulled first
				if (classDocs.isValidURL(href)) {
					//Class exists

					//Is it a class, method, or field

					final JavadocUrl javadocUrl = JavadocUrl.fromURL(href);

					final String className = javadocUrl.getClassName();

					final String targetAsText = getTargetAsText(seeAlsoClassElement, javadocUrl.getTargetType());
					final SeeAlsoReference ref = switch (javadocUrl.getTargetType()) {
						case CLASS -> new SeeAlsoReference(targetAsText, href, TargetType.CLASS, className);
						case METHOD -> new SeeAlsoReference(targetAsText, href, TargetType.METHOD, className + "#" + DocUtils.getSimpleSignature(javadocUrl.getFragment()));
						case FIELD -> new SeeAlsoReference(targetAsText, href, TargetType.FIELD, className + "#" + javadocUrl.getFragment());
						default -> throw new IllegalStateException("Unexpected javadoc target type: " + javadocUrl.getTargetType());
					};

					tryAddReference(ref);
				} else {
					tryAddReference(new SeeAlsoReference(seeAlsoClassElement.text(), href, TargetType.UNKNOWN, null));
				}
			} catch (Exception e) {
				LOGGER.error("An exception occurred while retrieving a 'See also' detail", e);
			}
		}
	}

	private void tryAddReference(SeeAlsoReference seeAlsoClassElement) {
		if (!references.contains(seeAlsoClassElement)) {
			references.add(seeAlsoClassElement);
		}
	}

	@NotNull
	private String getTargetAsText(Element seeAlsoClassElement, TargetType targetType) {
		final String text = seeAlsoClassElement.text();

		if (targetType != TargetType.METHOD) return text;

		final StringBuilder textBuilder = new StringBuilder(text);

		final int parenthesisIndex = text.indexOf('(');
		final int index = text.lastIndexOf('.', parenthesisIndex == -1
				? 0
				: parenthesisIndex);

		if (index > -1) {
			textBuilder.replace(index, index + 1, "#");
		}

		return textBuilder.toString();
	}

	public List<SeeAlsoReference> getReferences() {
		return references;
	}
}
