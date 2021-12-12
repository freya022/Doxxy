package com.freya02.bot.utils;

import com.freya02.bot.Main;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Utils {
	private static final Path CACHE_PATH = Main.BOT_FOLDER.resolve("docs_cache");
	private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
			.cache(new Cache(CACHE_PATH.toFile(), Long.MAX_VALUE))
			.build();

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
		return Jsoup.parse(downloadBody(url), url);
	}

	private static String downloadBody(String url) throws IOException {
		try (Response response = CLIENT.newCall(new Request.Builder()
				.url(url)
				.build()
		).execute()) {
			final ResponseBody body = response.body();
			if (body == null) throw new IllegalArgumentException("Got no body from url: " + url);

			return body.string();
		}
	}

	//In case OkHttp caching isn't accurate, use code below and transform it to use ETag(s)
//	private static Path changeExtension(Path path, String newExtension) {
//		final String fileName = path.getFileName().toString();
//
//		return path.resolveSibling(fileName.substring(0, fileName.lastIndexOf('.') + 1) + newExtension);
//	}

//	private static String downloadBody(@NotNull String url) throws IOException {
//		final HttpUrl httpUrl = HttpUrl.get(url);
//		Path filePath = CACHE_PATH.resolve(httpUrl.host());
//		for (String segment : httpUrl.pathSegments()) {
//			filePath = filePath.resolve(segment);
//		}
//
//		final Path lastModifiedPath = changeExtension(filePath, "lastmodified");
//		if (Files.exists(lastModifiedPath)) {
//			//Try to ask the server, and check the 304 status if the file has NOT changed
//
//			try (Response response = CLIENT.newCall(new Request.Builder()
//					.url(url)
//					.head()
//					.header("If-Modified-Since", Files.readString(lastModifiedPath))
//					.build()
//			).execute()) {
//				if (response.code() == 200) { //File modified
//					return updateDownloadedResource(url, filePath, lastModifiedPath, response);
//				} else if (response.code() == 304) { //File not modified, read from disk
//					return Files.readString(filePath);
//				} else {
//					throw new IllegalArgumentException("Unexpected HTTP code on if-modified call: " + response.code());
//				}
//			}
//		} else {
//			try (Response response = CLIENT.newCall(new Request.Builder()
//					.url(url)
//					.build()
//			).execute()) {
//				if (response.isSuccessful()) {
//					return updateDownloadedResource(url, filePath, lastModifiedPath, response);
//				} else {
//					throw new IllegalArgumentException("Unexpected HTTP code on initial get: " + response.code());
//				}
//			}
//		}
//	}
//
//	@NotNull
//	private static String updateDownloadedResource(@NotNull String url, Path filePath, Path lastModifiedPath, Response response) throws IOException {
//		final String body = downloadResponse(filePath, response);
//
//		final String lastModifiedHeader = response.header("Last-Modified");
//		if (lastModifiedHeader == null)
//			throw new IllegalArgumentException("Got no Last-Modified header from If-Modified-Since response on url " + url);
//
//		Files.writeString(lastModifiedPath, lastModifiedHeader, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
//
//		return body;
//	}
//
//	@NotNull
//	private static String downloadResponse(Path filePath, Response response) throws IOException {
//		String input;
//		final Path tempFile = Files.createTempFile("doc", ".html");
//
//		final ResponseBody body = response.body();
//		if (body == null) throw new IllegalArgumentException("Got no body for url: " + response.request().url());
//
//		input = body.string();
//		Files.writeString(tempFile, input, StandardOpenOption.WRITE);
//
//		Files.createDirectories(filePath.getParent());
//		Files.move(tempFile, filePath, StandardCopyOption.ATOMIC_MOVE);
//
//		return input;
//	}
}
