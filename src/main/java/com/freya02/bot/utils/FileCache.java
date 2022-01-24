package com.freya02.bot.utils;

import com.freya02.botcommands.api.Logging;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

public class FileCache implements Closeable {
	private static final Logger LOGGER = Logging.getLogger();
	private final boolean zip;
	private final Path outerPath;
	private final Path cachePath;

	public FileCache(Path where, String cacheName, boolean zip) throws IOException {
		this.zip = zip;

		if (!zip) {
			this.cachePath = outerPath = where.resolve(cacheName);
		} else {
			final Path zipPath = where.resolve(cacheName + ".zip");
			this.outerPath = zipPath;

			//https://docs.oracle.com/javase/7/docs/technotes/guides/io/fsp/zipfilesystemprovider.html
			final Map<String, String> env = Map.of("create", "true",
					"noCompression", "true");
			final URI uri = URI.create("jar:" + zipPath.toUri());

			this.cachePath = FileSystems.newFileSystem(uri, env).getPath("");
		}
	}

	public Path getOuterPath() {
		return outerPath;
	}

	public Path getCachePath() {
		return cachePath;
	}

	@Override
	public void close() throws IOException {
		if (zip) {
			LOGGER.info("Closed ZIP cache");

			cachePath.getFileSystem().close();
		}
	}
}
