package com.freya02.bot.docs.index;

import com.freya02.bot.utils.CryptoUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class IndexPaths {
	private final Path sourceCacheFolder;

	public IndexPaths(Path sourceCacheFolder) {
		this.sourceCacheFolder = sourceCacheFolder;
	}

	@NotNull
	public Path getClassEmbedPath(String className) {
		return sourceCacheFolder.resolve(className).resolve("ClassEmbed.json");
	}

	@NotNull
	public Path getMethodEmbedPath(String className, String methodId) {
		return sourceCacheFolder.resolve(className).resolve(getMethodFileName(methodId));
	}

	@NotNull
	public Path getFieldEmbedPath(String className, String fieldName) {
		return sourceCacheFolder.resolve(className).resolve(getFieldFileName(fieldName));
	}

	@NotNull
	public String getMethodFileName(@NotNull String signature) {
		return CryptoUtils.hash(signature) + ".json";
	}

	@NotNull
	public String getFieldFileName(String fieldName) {
		return CryptoUtils.hash(fieldName) + ".json";
	}
}
