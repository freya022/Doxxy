package com.freya02.bot.versioning;

import com.freya02.bot.utils.HttpUtils;
import net.dv8tion.jda.api.exceptions.ParsingException;
import net.dv8tion.jda.api.utils.data.DataArray;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

class VersionsUtils {
	public static final String MAVEN_METADATA_FORMAT = "https://repo.maven.apache.org/maven2/%s/%s/maven-metadata.xml";
	public static final String M2_METADATA_FORMAT = "https://m2.dv8tion.net/releases/%s/%s/maven-metadata.xml";

	@NotNull
	static String getLatestHash(String ownerName, String repoName, String branchName) throws IOException {
		final HttpUrl url = HttpUrl.get("https://api.github.com/repos/%s/%s/commits".formatted(ownerName, repoName))
				.newBuilder()
				.addQueryParameter("page", "1")
				.addQueryParameter("per_page", "1")
				.addQueryParameter("sha", branchName)
				.build();

		try (Response response = HttpUtils.CLIENT.newCall(new Request.Builder()
						.url(url)
						.header("Accept", "applications/vnd.github.v3+json")
						.build())
				.execute()) {

			final String json = response.body().string();

			return DataArray.fromJson(json).getObject(0).getString("sha").substring(0, 10);
		}
	}

	@NotNull
	static String getLatestMavenVersion(String formatUrl, String groupId, String artifactId) throws IOException {
		final Document document = getMavenMetadata(formatUrl, groupId, artifactId);

		final Element latestElement = document.selectFirst("metadata > versioning > latest");
		if (latestElement == null) throw new ParsingException("Unable to parse latest version");

		return latestElement.text();
	}

	@NotNull
	static Document getMavenMetadata(String formatUrl, String groupId, String artifactId) throws IOException {
		return HttpUtils.getDocument(formatUrl.formatted(groupId.replace('.', '/'), artifactId));
	}
}
