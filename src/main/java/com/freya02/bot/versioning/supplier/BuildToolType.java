package com.freya02.bot.versioning.supplier;

public enum BuildToolType {
	MAVEN("Maven"),
	GRADLE("Gradle"),
	GRADLE_KTS("Kotlin Gradle");

	private final String humanName;

	BuildToolType(String humanName) {
		this.humanName = humanName;
	}

	public String getHumanName() {
		return humanName;
	}
}
