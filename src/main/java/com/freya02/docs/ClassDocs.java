package com.freya02.docs;

import com.freya02.bot.utils.HttpUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ClassDocs {
	private static final Set<String> loaded = Collections.synchronizedSet(new HashSet<>());
	private static final Map<DocSourceType, ClassDocs> sourceMap = Collections.synchronizedMap(new EnumMap<>(DocSourceType.class));

	private final DocSourceType source;
	private final Set<String> blackList = new HashSet<>();
	private final Map<String, ClassDoc> urlToDocMap = new HashMap<>();
	private final Map<String, ClassDoc> simpleNameToDocMap = new HashMap<>();

	public ClassDocs(DocSourceType source) {
		this.source = source;
	}

	public static ClassDocs getSource(String url) {
		return getSource(DocSourceType.fromUrl(url));
	}

	public static ClassDocs getSource(DocSourceType source) {
		return sourceMap.computeIfAbsent(source, ClassDocs::new);
	}

	public Map<String, ClassDoc> getDocNamesMap() {
		return Collections.unmodifiableMap(simpleNameToDocMap);
	}

	public boolean nameExists(@NotNull String name) {
		return simpleNameToDocMap.containsKey(name);
	}

	public boolean urlExists(@NotNull String url) {
		final String cleanUrl = removeFragment(url);

		return cleanUrlExists(cleanUrl);
	}

	private boolean cleanUrlExists(String cleanUrl) {
		return urlToDocMap.containsKey(cleanUrl);
	}

	@Nullable
	public ClassDoc getByName(@NotNull String name) {
		return simpleNameToDocMap.get(name);
	}

	@Nullable
	public ClassDoc getOrNull(@NotNull String url) {
		url = removeFragment(url);

		if (!blackList.contains(url)) {
			try {
				return compute(url);
			} catch (Exception ignored) {
				blackList.add(url);
			}
		}

		return null;
	}

	@Nullable
	public ClassDoc compute(@NotNull String url) throws IOException {
		final DocSourceType urlSource = DocSourceType.fromUrl(url);
		if (urlSource != source) return null;
		if (urlSource == null) return null;

		url = removeFragment(url);

		if (!cleanUrlExists(url)) {
			final ClassDoc newDocs = new ClassDoc(url);

			urlToDocMap.put(url, newDocs);
			simpleNameToDocMap.put(newDocs.getClassName(), newDocs);

			return newDocs;
		} else {
			return urlToDocMap.get(url);
		}
	}

	/**
	 * This is nullable if the DocSource of this URL is not recognized
	 */
	@Nullable
	public static ClassDoc globalCompute(@NotNull String url) throws IOException {
		return getSource(url).compute(url);
	}

	@NotNull
	private static String removeFragment(@NotNull String url) {
		final int index = url.indexOf('#');
		if (index >= 0) {
			return url.substring(0, index);
		}

		return url;
	}

	public static synchronized ClassDocs loadAllDocs(String indexUrl) {
		if (!loaded.add(indexUrl)) return getSource(indexUrl);

		try {
			final Document document = HttpUtils.getDocument(indexUrl);

			final Map<String, ClassDoc> docsMap = new ConcurrentHashMap<>();

			//Multithreading is rather useless here... only saved 100 ms out of 400 of the time
			final ExecutorService service = Executors.newFixedThreadPool(4);
			for (Element element : document.select("#all-classes-table > div > div.summary-table.two-column-summary > div.col-first > a:nth-child(1)")) { //n = 1 needed as type parameters are links and external types
				service.submit(() -> {
					try {
						final ClassDoc docs = ClassDocs.globalCompute(element.absUrl("href"));

						final ClassDoc oldVal = docsMap.put(docs.getClassName(), docs);

						if (oldVal != null) {
							throw new IllegalStateException("Duplicated docs: " + element.absUrl("href"));
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}

			service.shutdown();
			service.awaitTermination(1, TimeUnit.DAYS);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Unable to find all docs", e);
		}

		return getSource(indexUrl);
	}
}
