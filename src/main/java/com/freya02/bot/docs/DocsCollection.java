package com.freya02.bot.docs;

import org.jsoup.internal.StringUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DocsCollection {
	private DocsCollection() {

	}

	public static DocsCollection of(String allClassesUrl) throws IOException {
		final ClassList list = ClassList.of(allClassesUrl);
		final Map<String, BasicDocs> docsMap = new HashMap<>();

		for (Map.Entry<String, String> entry : list.getClassToUrlMap().entrySet()) {
			String className = entry.getKey();
			String classRelativeUrl = entry.getValue();

			final String absoluteUrl = StringUtil.resolve(allClassesUrl, classRelativeUrl);
			docsMap.put(className, BasicDocs.of(absoluteUrl));
		}

		return new DocsCollection();
	}
}
