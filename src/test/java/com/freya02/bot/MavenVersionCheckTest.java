package com.freya02.bot;

import com.freya02.bot.docs.BuildStatus;
import com.freya02.bot.utils.HttpUtils;
import com.freya02.bot.versioning.ArtifactInfo;
import com.freya02.bot.versioning.VersionsUtils;
import com.google.gson.Gson;
import net.dv8tion.jda.api.exceptions.ParsingException;
import net.dv8tion.jda.api.utils.data.DataArray;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Map;

public class MavenVersionCheckTest {
	public static void main(String[] args) throws Exception {
//		System.out.println("getLatestMavenVersion(\"net.dv8tion\", \"JDA\") = " + getLatestMavenVersion("net.dv8tion", "JDA"));
//
//		final String latestCommitHash = getLatestCommitHash("com.github.freya022", "BotCommands", "2.3.0");
//		System.out.println("getLatestCommitHash(\"com.github.freya022\", \"BotCommands\", \"2.3.0\") = " + latestCommitHash);
//
//		triggerBuild(latestCommitHash);
//
//		System.out.println("getJDAVersion(\"2.3.0\") = " + getJDAVersion("2.3.0"));

//		final String hash = getLatestHash("freya022", "BotCommands", "2.3.0");
//
//		System.out.println("hash = " + hash);
//
//		BuildStatus buildStatus;
//		while ((buildStatus = triggerBuild(hash)) == BuildStatus.IN_PROGRESS) {
//			Thread.sleep(500);
//		}
//
//		System.out.println("buildStatus = " + buildStatus);

		final ArtifactInfo latestBotCommandsVersion = retrieveLatestBotCommandsVersion("2.3.0");

		final BuildStatus buildStatus = VersionsUtils.waitForBuild(VersionsUtils.getLatestHash("freya022", "BotCommands", "2.3.0"));

		if (buildStatus != BuildStatus.OK) {
			return;
		}

		final Path tempZip = Files.createTempFile("BotCommandsDocs", ".zip");
		VersionsUtils.downloadJitpackDocs(latestBotCommandsVersion, tempZip);

		final Path targetDocsFolder = Main.JAVADOCS_PATH.resolve("BotCommands");

		if (Files.exists(targetDocsFolder)) {
			for (Path path : Files.walk(targetDocsFolder).sorted(Comparator.reverseOrder()).toList()) {
				Files.deleteIfExists(path);
			}
		}

		try (FileSystem zfs = FileSystems.newFileSystem(tempZip)) {
			final Path zfsRoot = zfs.getPath("/");

			for (Path sourcePath : Files.walk(zfsRoot)
					.filter(Files::isRegularFile)
					.filter(p -> p.getFileName().toString().endsWith("html"))
					.toList()) {
				final Path targetPath = targetDocsFolder.resolve(zfsRoot.relativize(sourcePath).toString());

				Files.createDirectories(targetPath.getParent());
				Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
			}
		}

		Files.deleteIfExists(tempZip);

		System.exit(0);
	}

	@NotNull
	private static ArtifactInfo retrieveLatestBotCommandsVersion(String branchName) throws IOException {
		final String ownerName = "freya022";
		final String groupId = "com.github." + ownerName;
		final String artifactId = "BotCommands";

		return new ArtifactInfo(groupId,
				artifactId,
				getLatestHash(ownerName, artifactId, branchName));
	}

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

	private static String getJDAVersion(String branchName) throws IOException {
		final Document document = HttpUtils.getDocument("https://raw.githubusercontent.com/freya022/BotCommands/%s/pom.xml".formatted(branchName));

		final Element jdaArtifactElement = document.selectFirst("project > dependencies > dependency > artifactId:matches(JDA)");
		if (jdaArtifactElement == null) throw new ParsingException("Unable to get JDA artifact element");

		final Element jdaVersionElement = jdaArtifactElement.parent().selectFirst("version");
		if (jdaVersionElement == null) throw new ParsingException("Unable to get JDA version element");

		return jdaVersionElement.text();
	}

	@SuppressWarnings("unchecked")
	private static BuildStatus triggerBuild(String latestCommitHash) throws IOException {
		try (Response response = HttpUtils.CLIENT.newCall(new Request.Builder()
						.url("https://jitpack.io/api/builds/com.github.freya022/BotCommands/%s".formatted(latestCommitHash))
						.build())
				.execute()) {
			final Map<String, ?> map = new Gson().fromJson(response.body().string(), Map.class);

			final String status = (String) map.get("status");

			if (response.code() == 200 && status.equalsIgnoreCase("ok")) {
				return BuildStatus.OK;
			} else if (response.code() == 404 && status.equalsIgnoreCase("ok")) {
				return BuildStatus.IN_PROGRESS;
			} else if (response.code() == 404 && status.equalsIgnoreCase("error")) {
				return BuildStatus.ERROR;
			} else {
				throw new IllegalStateException("Unable to check build status: code = " + response.code() + ", status = '" + status + "'");
			}
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
}
