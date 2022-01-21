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

public class Versions {
	@NotNull
	private static String getLatestHash(String ownerName, String repoName, String branchName) throws IOException {
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

	private static String getLatestMavenVersion(String groupId, String artifactId) throws IOException {
		final Document document = getMavenMetadata(groupId, artifactId);

		final Element latestElement = document.selectFirst("metadata > versioning > latest");
		if (latestElement == null) throw new ParsingException("Unable to parse latest version");

		return latestElement.text();
	}

	@NotNull
	private static Document getMavenMetadata(String groupId, String artifactId) throws IOException {
		return HttpUtils.getDocument("https://repo.maven.apache.org/maven2/%s/%s/maven-metadata.xml".formatted(groupId.replace('.', '/'), artifactId));
	}

	@NotNull
	public static ArtifactInfo getJDAVersionFromBotCommands(String branchName) throws IOException {
		final Document document = HttpUtils.getDocument("https://raw.githubusercontent.com/freya022/BotCommands/%s/pom.xml".formatted(branchName));

		final Element jdaArtifactElement = document.selectFirst("project > dependencies > dependency > artifactId:matches(JDA)");
		if (jdaArtifactElement == null) throw new ParsingException("Unable to get JDA artifact element");

		final Element parent = jdaArtifactElement.parent();
		final Element jdaGroupIdElement = parent.selectFirst("groupId");
		if (jdaGroupIdElement == null) throw new ParsingException("Unable to get JDA group ID element");

		final Element jdaVersionElement = parent.selectFirst("version");
		if (jdaVersionElement == null) throw new ParsingException("Unable to get JDA version element");

		return new ArtifactInfo(
				jdaGroupIdElement.text(),
				"JDA",
				jdaVersionElement.text()
		);
	}

	@NotNull
	public static ArtifactInfo getLatestBotCommands(String branchName) throws IOException {
		final String ownerName = "freya022";
		final String groupId = "com.github." + ownerName;
		final String artifactId = "BotCommands";

		return new ArtifactInfo(groupId,
				artifactId,
				getLatestHash(ownerName, artifactId, branchName));
	}

	@NotNull
	public static ArtifactInfo getLatestJDAVersion() throws IOException {
		final String groupId = "net.dv8tion";
		final String artifactId = "JDA";

		return new ArtifactInfo(groupId,
				artifactId,
				getLatestMavenVersion(groupId, artifactId));
	}
}
