package com.freya02.bot.utils;

import com.freya02.bot.Main;
import com.freya02.botcommands.api.Logging;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class HttpUtils {
	public static final OkHttpClient CLIENT;

	private static final Logger LOGGER = Logging.getLogger();

	static {
		final Cache cache = new Cache(Main.CACHE_PATH.toFile(), Long.MAX_VALUE);

		CLIENT = new OkHttpClient.Builder()
				.cache(cache)
				.addInterceptor(chain -> {
					final Request request = chain.request();
					final Response response = chain.proceed(request);

					if (response.code() == 404) {
						final Iterator<String> urls = cache.urls();

						final String urlStr = request.url().toString();
						LOGGER.warn("{} threw a 404, removing from cache", urlStr);
						while (urls.hasNext()) {
							final String cachedUrl = urls.next();

							if (cachedUrl.equals(urlStr)) {
								LOGGER.debug("{} has been removed from the cache", urlStr);

								urls.remove();
								break;
							}
						}
					}

					return response;
				})
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

	@NotNull
	public static String downloadBody(String url) throws IOException {
		try (Response response = CLIENT.newCall(new Request.Builder()
				.url(url)
				.build()
		).execute()) {
			final ResponseBody body = response.body();
			if (body == null) throw new IllegalArgumentException("Got no body from url: " + url);

			return body.string();
		}
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

	public static boolean doesStartByLocalhost(String link) {
		return link.startsWith("http://localhost");
	}

	@NotNull
	public static String removeFragment(@NotNull String url) {
		final int index = url.indexOf('#');
		if (index >= 0) {
			return url.substring(0, index);
		}

		return url;
	}
}
