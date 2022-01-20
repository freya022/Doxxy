package com.freya02.bot.utils;

import java.nio.file.Path;

public class IOUtils {
	public static Path changeExtension(Path path, String newExt) {
		final String fileName = path.getFileName().toString();

		return path.resolveSibling(fileName.substring(0, fileName.lastIndexOf('.') + 1) + newExt);
	}
}
