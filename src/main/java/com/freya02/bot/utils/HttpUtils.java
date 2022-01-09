package com.freya02.bot.utils;

import com.freya02.bot.Main;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class HttpUtils {
	private static final OkHttpClient CLIENT;

	static {
		CLIENT = new OkHttpClient.Builder()
				.cache(new Cache(Main.CACHE_PATH.toFile(), Long.MAX_VALUE))
				.build();
	}

	@NotNull
	public static synchronized Document parseDocument(@NotNull String downloadedBody, String baseUri) {
		return Jsoup.parse(downloadedBody, baseUri);
	}

	@NotNull
	public static synchronized Document getDocument(@NotNull String url) throws IOException {
		return parseDocument(downloadBody(url), url);
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

	public static Path getCachePathForUrl(String url) {
		final HttpUrl httpUrl = HttpUrl.get(url);

		Path path = Main.CACHE_PATH.resolve(httpUrl.host());
		for (String segment : httpUrl.pathSegments()) {
			path = path.resolve(segment);
		}

		return path;
	}

	/**
	 * Returns null if cache is still OK
	 */
	@Nullable
	public static String downloadBodyIfNotCached(String url) throws IOException {
		try (Response response = CLIENT.newCall(new Request.Builder()
				.cacheControl(new CacheControl.Builder() //TODO check if it works correctly
						.maxAge(0, TimeUnit.SECONDS)
						.build())
				.url(url)
				.build()
		).execute()) {
			if (response.cacheResponse() != null) return null;

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
