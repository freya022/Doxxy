package com.freya02.bot.versioning;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class VersionChecker {
	private final Path lastSavedPath;
	protected ArtifactInfo latest; //TODO cache latest and write once version update has completed, save to file.

	protected VersionChecker(Path lastSavedPath) throws IOException {
		this.lastSavedPath = lastSavedPath;

		this.latest = ArtifactInfo.fromFileString(lastSavedPath);
	}

	public abstract boolean checkVersion() throws IOException;

	public void saveVersion() throws IOException {
		Files.writeString(lastSavedPath, latest.toFileString());
	}

	public ArtifactInfo getLatest() {
		return latest;
	}
}
