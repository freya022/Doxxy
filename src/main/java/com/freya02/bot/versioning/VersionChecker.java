package com.freya02.bot.versioning;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class VersionChecker {
	private final Path lastSavedPath;
	protected ArtifactInfo latest;
	protected ArtifactInfo diskLatest;

	protected VersionChecker(Path lastSavedPath) throws IOException {
		this.lastSavedPath = lastSavedPath;

		this.latest = this.diskLatest = ArtifactInfo.fromFileString(lastSavedPath);
	}

	public abstract boolean checkVersion() throws IOException;

	public void saveVersion() throws IOException {
		Files.writeString(lastSavedPath, latest.toFileString());

		diskLatest = latest;
	}

	public ArtifactInfo getLatest() {
		return latest;
	}
}
