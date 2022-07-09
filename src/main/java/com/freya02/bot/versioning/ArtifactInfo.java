package com.freya02.bot.versioning;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record ArtifactInfo(String groupId, String artifactId, String version) {
	public String toFileString() {
		return String.join("\n", groupId, artifactId, version);
	}

	public String toJitpackUrl() {
		return "https://jitpack.io/%s/%s/%s/BotCommands-%s-javadoc.jar".formatted(
				groupId.replace('.', '/'),
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