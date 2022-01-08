package com.freya02.docs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum DocSourceType {
//	JDA(null), //TODO add JDA link once they are built with javadoc 17
	BOT_COMMANDS("http://localhost:63342/DocsBot/test_docs");
//	JAVA("https://docs.oracle.com/en/java/javase/17/docs/api");

	private final String sourceUrl;

	DocSourceType(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	@Nullable
	public static DocSourceType fromUrl(String url) {
		for (DocSourceType source : values()) {
			if (url.startsWith(source.getSourceUrl())) {
				return source;
			}
		}

		return null;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	@NotNull
	public String getAllClassesIndexURL() {
		return sourceUrl + "/allclasses-index.html";
	}
}
