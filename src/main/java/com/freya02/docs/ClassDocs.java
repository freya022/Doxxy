package com.freya02.docs;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ClassDocs {
	private static final Map<String, ClassDoc> docs = new HashMap<>();

	@NotNull
	public static ClassDoc of(@NotNull String url) throws IOException {
		final int index = url.indexOf('#');
		if (index >= 0) {
			url = url.substring(0, index);
		}

		if (docs.get(url) == null) {
			final ClassDoc newDocs = new ClassDoc(url);

			docs.put(url, newDocs);

			return newDocs;
		} else {
			return docs.get(url);
		}
	}
}
