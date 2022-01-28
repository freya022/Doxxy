package com.freya02.docs;

import com.freya02.bot.utils.DecomposedName;
import com.freya02.bot.utils.HttpUtils;
import com.freya02.botcommands.api.Logging;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;

public class ClassDocs {
	private static final Logger LOGGER = Logging.getLogger();
	private static final Map<DocSourceType, ClassDocs> sourceMap = Collections.synchronizedMap(new EnumMap<>(DocSourceType.class));

	private final DocSourceType source;

	private final Map<String, String> simpleNameToUrlMap = new HashMap<>();
	private final Set<String> urlSet = new HashSet<>();

	//Use this map in case there are multiple classes with the same name, use a small package prefix to differentiate them
//	private final Map<String, String> nameToUrlMap = new HashMap<>();

	private ClassDocs(DocSourceType source) {
		this.source = source;
	}

	@NotNull
	public static synchronized ClassDocs getSource(DocSourceType source) {
		return sourceMap.computeIfAbsent(source, ClassDocs::new);
	}

	@NotNull
	public static synchronized ClassDocs getUpdatedSource(DocSourceType source) throws IOException {
		final ClassDocs classDocs = sourceMap.computeIfAbsent(source, ClassDocs::new);

		classDocs.tryIndexAll();

		return classDocs;
	}

	public Map<String, String> getSimpleNameToUrlMap() {
		return simpleNameToUrlMap;
	}

	public boolean isValidURL(String url) {
		final String cleanURL = HttpUtils.removeFragment(url);

		return urlSet.contains(cleanURL);
	}

	private synchronized void tryIndexAll() throws IOException {
		final String indexURL = source.getAllClassesIndexURL();

		final String body;
		if (!simpleNameToUrlMap.isEmpty()) {
			//We try to "abuse" the OkHttp caching in order to determine if the name-to-URL cache needs to be updated
			// This allows SeeAlso to get updated sources of all DocSourceType(s) without spamming requests & parsing a lot

			body = HttpUtils.downloadBodyIfNotCached(indexURL);
			if (body == null) return;
		} else {
			body = HttpUtils.downloadBody(indexURL); //If this is the first run (map empty) then always download
		}

		LOGGER.info("Parsing ClassDocs URLs for: {}", source);

		final Document document = HttpUtils.parseDocument(body, indexURL);

		simpleNameToUrlMap.clear();
		urlSet.clear();

		//n = 1 needed as type parameters are links and external types
		// For example in AbstractComponentBuilder<T extends AbstractComponentBuilder<T>>
		// It could have selected 4 different URLs, except there is only 1 class we want here
		// Since it's the left most, it's easy to pick the first one
		for (Element element : document.select("#all-classes-table > div > div.summary-table.two-column-summary > div.col-first > a:nth-child(1)")) {
			final String classUrl = element.absUrl("href");

			final String rightPart = HttpUtils.removeFragment(classUrl.substring(source.getSourceUrl().length() + 1, classUrl.lastIndexOf('.')));

			final DecomposedName decomposition = DecomposedName.getDecomposition(rightPart.replace('/', '.'));

			if (!source.isValidPackage(decomposition.packageName()))
				continue;

			urlSet.add(classUrl);
			final String oldUrl = simpleNameToUrlMap.put(decomposition.className(), classUrl);
			if (oldUrl != null) {
				LOGGER.warn("Detected a duplicate class name '{}' at '{}' and '{}'", decomposition.className(), classUrl, oldUrl);
			}
		}
	}
}
