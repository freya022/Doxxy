package com.freya02.bot.utils;

import com.freya02.bot.Main;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public class Utils {
	private static final Path CACHE_PATH = Main.BOT_FOLDER.resolve("docs_cache");

	static {
		if (Files.notExists(CACHE_PATH)) {
			try {
				Files.createDirectory(CACHE_PATH);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static String fixJDKUrl(String url) {
		return url.replace("https://docs.oracle.com/javase/8", "https://docs.oracle.com/javase/9");
	}

	@NotNull
	public static String getClassName(String fullName) {
		for (int i = 0, length = fullName.length(); i < length; i++) {
			final char c = fullName.charAt(i);

			if (Character.isUpperCase(c)) {
				return fullName.substring(i);
			}
		}

		throw new IllegalArgumentException("Could not get glass name from '" + fullName + "'");
	}

	@NotNull
	public static synchronized Document getDocument(@NotNull String url) throws IOException {
		final String input;

		final HttpUrl httpUrl = HttpUrl.get(url);
		Path path = CACHE_PATH.resolve(httpUrl.host());
		for (String segment : httpUrl.pathSegments()) {
			path = path.resolve(segment);
		}

		if (Files.exists(path)) {
			input = Files.readString(path);
		} else {
			try (InputStream stream = new URL(url).openStream()) {
				input = new String(stream.readAllBytes());
			}

			final Path tempFile = Files.createTempFile("doc", ".html");

			Files.writeString(tempFile, input, StandardOpenOption.WRITE);

			Files.createDirectories(path.getParent());
			Files.move(tempFile, path, StandardCopyOption.ATOMIC_MOVE);
		}

		return Jsoup.parse(input, url);
	}
}
