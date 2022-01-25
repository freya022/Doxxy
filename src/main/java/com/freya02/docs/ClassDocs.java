package com.freya02.docs;

import com.freya02.bot.utils.DecomposedName;
import com.freya02.bot.utils.HttpUtils;
import com.freya02.botcommands.api.Logging;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class ClassDocs {
	private static final Logger LOGGER = Logging.getLogger();
	private static final Map<DocSourceType, ClassDocs> sourceMap = Collections.synchronizedMap(new EnumMap<>(DocSourceType.class));

	private final DocSourceType source;

	private final Map<String, String> simpleNameToUrlMap = new HashMap<>();

	//Use this map in case there are multiple classes with the same name, use a small package prefix to differentiate them
//	private final Map<String, String> nameToUrlMap = new HashMap<>();

	private ClassDocs(DocSourceType source) {
		this.source = source;
	}

	public static ClassDocs getSource(String url) {
		return getSource(DocSourceType.fromUrl(url));
	}

	public static ClassDocs getSource(DocSourceType source) {
		return sourceMap.computeIfAbsent(source, ClassDocs::new);
	}

	public Map<String, String> getSimpleNameToUrlMap() {
		return simpleNameToUrlMap;
	}

	/**
	 * Null if unsupported source
	 */
	@Nullable
	public static ClassDoc download(@NotNull String url) throws IOException {
		final DocSourceType urlSource = DocSourceType.fromUrl(url);
		if (urlSource == null) return null;

		url = removeFragment(url);

		final Document document = HttpUtils.getDocument(url);
		if (!ClassDoc.isJavadocVersionCorrect(document)) return null;

		return new ClassDoc(url, document);
	}

	@NotNull
	private static String removeFragment(@NotNull String url) {
		final int index = url.indexOf('#');
		if (index >= 0) {
			return url.substring(0, index);
		}

		return url;
	}

	public static synchronized ClassDocs indexAll(DocSourceType sourceType) throws IOException {
		final ClassDocs docs = new ClassDocs(sourceType);
		docs.indexAll();

		sourceMap.put(sourceType, docs);

		return docs;
	}

	private synchronized void indexAll() throws IOException {
		final String indexURL = source.getAllClassesIndexURL();

		final Document document = HttpUtils.getDocument(indexURL);

		simpleNameToUrlMap.clear();

		//n = 1 needed as type parameters are links and external types
		// For example in AbstractComponentBuilder<T extends AbstractComponentBuilder<T>>
		// It could have selected 4 different URLs, except there is only 1 class we want here
		// Since it's the left most, it's easy to pick the first one
		for (Element element : document.select("#all-classes-table > div > div.summary-table.two-column-summary > div.col-first > a:nth-child(1)")) {
			final String classUrl = element.absUrl("href");

			final String rightPart = removeFragment(classUrl.substring(source.getSourceUrl().length() + 1, classUrl.lastIndexOf('.')));

			final DecomposedName decomposition = DecomposedName.getDecomposition(rightPart.replace('/', '.'));

			if (!source.isValidPackage(decomposition.packageName()))
				continue;

			final String oldUrl = simpleNameToUrlMap.put(decomposition.className(), classUrl);
			if (oldUrl != null) {
				LOGGER.warn("Detected a duplicate class name '{}' at '{}' and '{}'", decomposition.className(), classUrl, oldUrl);
			}
		}
	}

	@Nullable
	public ClassDoc tryRetrieveDoc(String simpleClassName, boolean force) throws IOException {
		final String classUrl = simpleNameToUrlMap.get(simpleClassName);

		if (classUrl == null)
			throw new IllegalArgumentException(simpleClassName + " is not a valid class name");

		if (force) {
			return new ClassDoc(classUrl);
		} else {
			final String downloadedBody = HttpUtils.downloadBodyIfNotCached(classUrl);

			if (downloadedBody == null) return null;

			final Document document = HttpUtils.parseDocument(downloadedBody, classUrl);

			return new ClassDoc(classUrl, document);
		}
	}
}
