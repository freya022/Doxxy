package com.freya02.docs;

import org.jetbrains.annotations.Nullable;

public enum DocSourceType {
	JDA(), //TODO add JDA link once they are built with javadoc 16
	BOT_COMMANDS("http://localhost:63342/DocsBot/test_docs"),
	JAVA("https://docs.oracle.com/en/java/javase/16/docs/api");

	private final String[] sourceUrls;

	DocSourceType(String... sourceUrls) {
		this.sourceUrls = sourceUrls;
	}

	@Nullable
	public static DocSourceType fromUrl(String url) {
		for (DocSourceType source : values()) {
			for (String sourceUrl : source.sourceUrls) {
				if (url.startsWith(sourceUrl)) {
					return source;
				}
			}
		}

		return null;
	}
}
