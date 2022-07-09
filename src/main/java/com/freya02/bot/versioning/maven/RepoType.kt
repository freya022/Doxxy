package com.freya02.bot.versioning.maven;

public enum RepoType {
	M2("https://m2.dv8tion.net/releases/%s/%s/maven-metadata.xml"),
	MAVEN("https://repo.maven.apache.org/maven2/%s/%s/maven-metadata.xml");

	private final String urlFormat;

	RepoType(String urlFormat) {
		this.urlFormat = urlFormat;
	}

	public String getUrlFormat() {
		return urlFormat;
	}
}
