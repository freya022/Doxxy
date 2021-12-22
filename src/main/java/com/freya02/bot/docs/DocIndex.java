package com.freya02.bot.docs;

import com.freya02.botcommands.api.Logging;
import com.freya02.docs.ClassDoc;
import com.freya02.docs.ClassDocs;
import com.freya02.docs.MethodDoc;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class DocIndex {
	private static final Logger LOGGER = Logging.getLogger();

	private final ClassDocs classDocs;
	private final SimpleNameList simpleNameList = new SimpleNameList();

	//Performance could be better without nested maps but this is way easier for the autocompletion to understand what to show
	//Map<Class name, Map<Method short signature, MethodDoc>>
	private final SimpleNameMap<MethodIdMap> simpleNameToMethodsMap = new SimpleNameMap<>();

	public DocIndex(String indexUrl) {
		this.classDocs = ClassDocs.getSource(indexUrl);
		if (classDocs == null) throw new IllegalArgumentException("No ClassDocs source for " + indexUrl);

		LOGGER.info("Loading docs for {}", indexUrl);
		final ClassDocs classDocs = ClassDocs.loadAllDocs(indexUrl);
		LOGGER.info("Docs loaded for {}", indexUrl);

		for (ClassDoc doc : classDocs.getDocNamesMap().values()) {
			simpleNameList.add(doc.getClassName());

			final MethodIdMap methodIdMap = new MethodIdMap();

			for (MethodDoc methodDoc : doc.getMethodDocs().values()) {
				methodIdMap.put(methodDoc.getSimpleSignature(), methodDoc);
			}

			simpleNameToMethodsMap.put(doc.getClassName(), methodIdMap);
		}
	}

	@Nullable
	public MethodDoc getMethodDoc(String className, String methodId) {
		final MethodIdMap methodMap = simpleNameToMethodsMap.get(className);
		if (methodMap == null) return null;

		return methodMap.get(methodId);
	}

	@Nullable
	public ClassDoc getClassDoc(String className) {
		return classDocs.getByName(className);
	}

	@Nullable
	public Set<String> getMethodDocSuggestions(String className) {
		final MethodIdMap methodMap = simpleNameToMethodsMap.get(className);
		if (methodMap == null) return null;

		return methodMap.keySet();
	}

	public List<String> getSimpleNameList() {
		return simpleNameList;
	}

	public SimpleNameMap<MethodIdMap> getSimpleNameToMethodsMap() {
		return simpleNameToMethodsMap;
	}

	private static class SimpleNameList extends ArrayList<String> {}

	private static class SimpleNameMap<V> extends HashMap<String, V> {}

	private static class MethodIdMap extends HashMap<String, MethodDoc> {}
}
