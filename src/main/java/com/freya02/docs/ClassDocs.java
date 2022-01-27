package com.freya02.docs;

import com.freya02.bot.utils.DecomposedName;
import com.freya02.bot.utils.HttpUtils;
import com.freya02.botcommands.api.Logging;
import com.freya02.docs.data.ClassDoc;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
	public static ClassDocs getSource(String url) throws IOException {
		return getSource(DocSourceType.fromUrl(url));
	}

	@NotNull
	public static synchronized ClassDocs getSource(DocSourceType source) throws IOException {
		final ClassDocs classDocs = sourceMap.computeIfAbsent(source, ClassDocs::new);

		classDocs.tryIndexAll();

		return classDocs;
	}

	public Map<String, String> getSimpleNameToUrlMap() {
		return simpleNameToUrlMap;
	}

	public boolean isValidURL(String url) {
		final String cleanURL = removeFragment(url);

		return urlSet.contains(cleanURL);
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

			final String rightPart = removeFragment(classUrl.substring(source.getSourceUrl().length() + 1, classUrl.lastIndexOf('.')));

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

	//TODO Use doc download "sessions" as to avoid re-downloading and re-parsing docs, for example when traversing superclasses
	// Proof: there are 645 documented classes in JDA + BC, but ClassDoc.<init> got called around 4500 times
	// ClassDocs is not a session, sessions should be disposable, meanwhile ClassDocs is stored statically -> ClassDoc(s) stored indefinitely
	//   Could pass the docs session to all our objects which requires references to other classes
	// ClassDocs would only remain as a container for Name to URL mappings, and retrieval operations would be on the session

	/**
	 * Only used to determine whether to regenerate doc indexes
	 */
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
