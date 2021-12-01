package com.freya02.bot.docs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public record DocsCollection(ConstantList constantList, Map<String, BasicDocs> docsMap) {
	public static DocsCollection of(String allClassesUrl, String constantsUrl) throws IOException {
		ClassList.runGeneration(allClassesUrl);

		final ConstantList constantList = ConstantList.of(constantsUrl);
		final Map<String, BasicDocs> docsMap = new HashMap<>();

		for (Map.Entry<String, ClassReference> entry : ClassReferences.getAllReferences().entrySet()) {
			String className = entry.getKey();
			ClassReference classRef = entry.getValue();

			final String link = classRef.link();
			if (link == null) {
				continue;
			}

			if (docsMap.put(className, new BasicDocs(link)) != null) {
				throw new IllegalStateException("Overwrote " + className + ": " + link);
			}
		}

		return new DocsCollection(constantList, docsMap);
	}
}
