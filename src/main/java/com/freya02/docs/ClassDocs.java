package com.freya02.docs;

import com.freya02.bot.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ClassDocs {
	private static final Map<String, ClassDoc> docs = new HashMap<>();
	private static final Map<String, ClassDoc> docNames = new HashMap<>();
	private static boolean loaded = false;

	public static Map<String, ClassDoc> getDocNamesMap() {
		return Collections.unmodifiableMap(docNames); //TODO should use singleton
	}

	@Nullable
	public static ClassDoc ofName(@NotNull String name) {
		return docNames.get(name);
	}

	@NotNull
	public static ClassDoc of(@NotNull String url) throws IOException {
		final int index = url.indexOf('#');
		if (index >= 0) {
			url = url.substring(0, index);
		}

		if (docs.get(url) == null) {
			final ClassDoc newDocs = new ClassDoc(url);

			docs.put(url, newDocs);
			docNames.put(newDocs.getClassName(), newDocs);

			return newDocs;
		} else {
			return docs.get(url);
		}
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
			for (Element element : document.select("body > main > ul > li > a")) {
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
