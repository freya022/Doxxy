package com.freya02.bot.versioning;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record ArtifactInfo(String groupId, String artifactId, String version) {
	public static final String MAVEN_JAVADOC_FORMAT = "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s-javadoc.jar";
	public static final String JITPACK_JAVADOC_FORMAT = "https://jitpack.io/%s/%s/%s/%s-%s-javadoc.jar";

	public String toFileString() {
		return String.join("\n", groupId, artifactId, version);
	}

	@NotNull
	public String toMavenJavadocUrl() {
		return MAVEN_JAVADOC_FORMAT.formatted(
				groupId.replace('.', '/'),
				artifactId,
				artifactId,
				version,
				version
		);
	}

	@NotNull
	public String toJitpackJavadocUrl() {
		return JITPACK_JAVADOC_FORMAT.formatted(
				groupId.replace('.', '/'),
				artifactId,
				artifactId,
				version,
				version
		);
	}

	public static ArtifactInfo fromFileString(Path path) throws IOException {
		if (Files.notExists(path)) {
			return new ArtifactInfo("invalid", "invalid", "invalid");
		}

		final List<String> lines = Files.readAllLines(path);
		if (lines.size() != 3) {
			return new ArtifactInfo("invalid", "invalid", "invalid");
		}

		return new ArtifactInfo(
				lines.get(0),
				lines.get(1),
				lines.get(2)
		);
	}
}