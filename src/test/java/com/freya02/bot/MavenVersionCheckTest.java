package com.freya02.bot;

import net.dv8tion.jda.api.exceptions.ParsingException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

public class MavenVersionCheckTest {
	private static final String MAVEN_FORMAT = "https://repo.maven.apache.org/maven2/%s/%s/maven-metadata.xml";
	private static final String JITPACK_FORMAT = "https://repo.maven.apache.org/maven2/%s/%s/maven-metadata.xml";
	private static final OkHttpClient CLIENT = new OkHttpClient.Builder().build();

	public static void main(String[] args) throws Exception {
		System.out.println("getLatestVersion(\"net.dv8tion\", \"JDA\") = " + getLatestVersion(MAVEN_FORMAT, "net.dv8tion", "JDA"));

		System.exit(0);
	}

	private static String getLatestVersion(String format, String groupId, String artifactId) throws IOException {
		try (Response response = callMvnRepo(format, groupId, artifactId)) {
			final String string = response.body().string();

			final Document document = Jsoup.parse(string);

			final Element latestElement = document.selectFirst("metadata > versioning > latest");
			if (latestElement == null) throw new ParsingException("Unable to parse latest version");

			return latestElement.text();
		}
	}

	@NotNull
	private static Response callMvnRepo(String format, String groupId, String artifactId) throws IOException {
		return CLIENT.newCall(new Request.Builder()
						.url(format.formatted(groupId.replace('.', '/'), artifactId))
				.build()).execute();
	}
}
