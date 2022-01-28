package com.freya02.docs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum DocSourceType {
//	JDA(null), //TODO add JDA link once they are built with javadoc 17
	JDA("http://localhost:63342/DocsBot/JDA_docs"),
	BOT_COMMANDS("http://localhost:63342/DocsBot/BotCommands_docs", "com.freya02.botcommands.api"),
	JAVA("https://docs.oracle.com/en/java/javase/17/docs/api", "java");

	private final String sourceUrl;
	private final String[] validPackages;

	DocSourceType(String sourceUrl, String... validPackages) {
		this.sourceUrl = sourceUrl;
		this.validPackages = validPackages.length == 0
				? new String[]{""}
				: validPackages;
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

	public boolean isValidPackage(String packageName) {
		for (String validPackage : validPackages) {
			if (packageName.startsWith(validPackage)) {
				return true;
			}
		}

		return false;
	}
}
