package com.freya02.docs;

import com.freya02.bot.utils.Utils;
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
	private static final Set<String> blackList = new HashSet<>();
	private static final Map<String, ClassDoc> docs = new HashMap<>();
	private static final Map<String, ClassDoc> docNames = new HashMap<>();
	private static boolean loaded = false;

	public static Map<String, ClassDoc> getDocNamesMap() {
		return Collections.unmodifiableMap(docNames); //TODO should use singleton
	}

	public static boolean nameExists(@NotNull String name) {
		return docNames.containsKey(name);
	}

	public static boolean urlExists(@NotNull String url) {
		final String cleanUrl = removeFragment(url);

		return cleanUrlExists(cleanUrl);
	}

	private static boolean cleanUrlExists(String cleanUrl) {
		return docs.containsKey(cleanUrl);
	}

	@Nullable
	public static ClassDoc ofName(@NotNull String name) {
		return docNames.get(name);
	}

	@Nullable
	public static ClassDoc getOrNull(@NotNull String url) {
		url = removeFragment(url);

		if (!blackList.contains(url)) {
			try {
				return of(url);
			} catch (Exception ignored) {
				blackList.add(url);
			}
		}

		return null;
	}

	@NotNull
	public static ClassDoc of(@NotNull String url) throws IOException {
		url = removeFragment(url);

		if (!cleanUrlExists(url)) {
			final ClassDoc newDocs = new ClassDoc(url);

			docs.put(url, newDocs);
			docNames.put(newDocs.getClassName(), newDocs);

			return newDocs;
		} else {
			return docs.get(url);
		}
	}

	@NotNull
	private static String removeFragment(@NotNull String url) {
		final int index = url.indexOf('#');
		if (index >= 0) {
			return url.substring(0, index);
		}

		return url;
	}

	public static synchronized void loadAllDocs(String url) {
		if (loaded) {
			return;
		}

		loaded = true;

		try {
			final Document document = Utils.getDocument(url);

			final Map<String, ClassDoc> docsMap = new ConcurrentHashMap<>();

			//Multithreading is rather useless here... only saved 100 ms out of 400 of the time
			final ExecutorService service = Executors.newFixedThreadPool(4);
			for (Element element : document.select("#all-classes-table > div > div.summary-table.two-column-summary > div.col-first > a:nth-child(1)")) { //n = 1 needed as type parameters are links and external types
				service.submit(() -> {
					try {
						final ClassDoc docs = ClassDocs.of(element.absUrl("href"));

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
	}
}
