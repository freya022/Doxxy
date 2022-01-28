package com.freya02.docs;

import com.freya02.bot.utils.HttpUtils;
import com.freya02.docs.data.ClassDoc;
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
	 * Null if unsupported source or invalid javadoc version
	 */
	@Nullable
	public ClassDoc retrieveDoc(@NotNull String classUrl) {
		return docMap.computeIfAbsent(classUrl, s -> {
			try {
				final DocSourceType urlSource = DocSourceType.fromUrl(classUrl);
				if (urlSource == null) return null;

				final Document document = HttpUtils.getDocument(classUrl);
				if (!DocUtils.isJavadocVersionCorrect(document)) return null;

				return new ClassDoc(this, HttpUtils.removeFragment(classUrl), document);
			} catch (IOException e) {
				throw new RuntimeException("Error while computing value for doc @ " + classUrl, e);
			}
		});
	}

	/**
	 * Null if unsupported source or invalid javadoc version or html body cached
	 */
	@Nullable
	public ClassDoc retrieveDocIfNotCached(@NotNull String classUrl) throws IOException {
		final String downloadedBody = HttpUtils.downloadBodyIfNotCached(classUrl);

		if (downloadedBody == null) return null;

		final Document document = HttpUtils.parseDocument(downloadedBody, classUrl);

		final ClassDoc newDoc = new ClassDoc(this, classUrl, document);

		docMap.put(classUrl, newDoc);

		return newDoc;
	}

//	/**
//	 * Only used to determine whether to regenerate doc indexes
//	 */
//	@Nullable
//	public ClassDoc tryRetrieveDoc(@NotNull String classUrl, boolean force) {
//		if (force) {
//			return retrieveDoc(classUrl);
//		} else {
//			return docMap.computeIfAbsent(classUrl, x -> {
//				try {
//					return new ClassDoc(classUrl);
//				} catch (IOException e) {
//					throw new RuntimeException("", e);
//				}
//			});
//		}
//	}
}
