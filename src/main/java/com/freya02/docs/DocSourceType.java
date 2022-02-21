package com.freya02.docs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.regex.Pattern;

public enum DocSourceType {
	JDA("http://localhost:25566/JDA", "https://ci.dv8tion.net/job/JDA5/javadoc", "net\\.dv8tion\\.jda.*"),
	BOT_COMMANDS("http://localhost:25566/BotCommands", null, "com\\.freya02\\.botcommands\\.api.*"),
	JAVA("https://docs.oracle.com/en/java/javase/17/docs/api",
			"https://docs.oracle.com/en/java/javase/17/docs/api",
			"java\\.io.*",
			"java\\.lang", "java\\.lang\\.annotation.*", "java\\.lang\\.invoke.*", "java\\.lang\\.reflect.*", "java\\.lang\\.reflect.*",
			"java\\.math.*",
			"java\\.nio", "java\\.nio\\.channels", "java\\.nio\\.file",
			"java\\.sql.*",
			"java\\.time.*",
			"java\\.util", "java\\.util\\.concurrent.*", "java\\.util\\.function", "java\\.util\\.random", "java\\.util\\.regex", "java\\.util\\.stream");

	private final String sourceUrl;
	private final String onlineURL;
	private final Pattern[] validPackagePatterns;

	DocSourceType(String sourceUrl, String onlineURL, String... validPackagePatterns) {
		this.sourceUrl = sourceUrl;
		this.onlineURL = onlineURL;
		this.validPackagePatterns = Arrays.stream(validPackagePatterns).map(Pattern::compile).toArray(Pattern[]::new);
	}

	@Nullable
	public static DocSourceType fromUrl(String url) {
		for (DocSourceType source : values()) {
			if (url.startsWith(source.getSourceUrl()) || (source.getOnlineURL() != null && url.startsWith(source.getOnlineURL()))) {
				return source;
			}
		}

		return null;
	}

	private String getOnlineURL() {
		return onlineURL;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	@NotNull
	public String toOnlineURL(String url) {
		if (onlineURL == null) return url;

		if (url.startsWith(sourceUrl)) {
			return onlineURL + url.substring(sourceUrl.length());
		} else {
			return url;
		}
	}

	@NotNull
	public String getAllClassesIndexURL() {
		return sourceUrl + "/allclasses-index.html";
	}

	public boolean isValidPackage(String packageName) {
		for (Pattern validPackagePattern : validPackagePatterns) {
			if (validPackagePattern.matcher(packageName).matches()) {
				return true;
			}
		}

		return false;
	}
}
