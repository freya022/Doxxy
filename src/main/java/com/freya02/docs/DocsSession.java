package com.freya02.docs;

import com.freya02.bot.utils.HttpUtils;
import com.freya02.docs.data.ClassDoc;
import com.freya02.docs2.PageCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DocsSession {
	/**
	 * URL to ClassDoc
	 */
	private final Map<String, ClassDoc> docMap = new HashMap<>();

	/**
	 * Retrieves the ClassDoc for this URL
	 *
	 * @param classUrl URL of the class
	 * @return <code>null</code> if:
	 * <ul>
	 *     <li>The URL's DocSourceType is not known</li>
	 *     <li>The javadoc version is incorrect</li>
	 * </ul>
	 * <p>
	 * Always returns a ClassDoc otherwise
	 */
	@Nullable
	public ClassDoc retrieveDoc(@NotNull String classUrl) throws IOException {
		synchronized (docMap) {
			final ClassDoc doc = docMap.get(classUrl); //Can't use computeIfAbsent as it could be recursively called, throwing a ConcurrentModificationException

			if (doc == null) {
				final DocSourceType urlSource = DocSourceType.fromUrl(classUrl);
				if (urlSource == null) return null;

				final Document document = PageCache.INSTANCE.getPage(classUrl);
				if (!DocUtils.isJavadocVersionCorrect(document)) return null;

				final ClassDoc classDoc = new ClassDoc(this, HttpUtils.removeFragment(classUrl), document);

				docMap.put(classUrl, classDoc);

				return classDoc;
			}

			return doc;
		}
	}
}
