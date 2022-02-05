package com.freya02.bot.versioning;

import com.freya02.bot.utils.HttpUtils;
import com.freya02.bot.versioning.github.GithubBranch;
import com.freya02.bot.versioning.jitpack.BuildStatus;
import com.google.gson.Gson;
import net.dv8tion.jda.api.exceptions.ParsingException;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.nio.file.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class VersionsUtils {
	public static final String MAVEN_METADATA_FORMAT = "https://repo.maven.apache.org/maven2/%s/%s/maven-metadata.xml";
	public static final String MAVEN_JAVADOC_FORMAT = "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s-javadoc.jar";
	public static final String M2_METADATA_FORMAT = "https://m2.dv8tion.net/releases/%s/%s/maven-metadata.xml";

	@NotNull
	public static String getLatestHash(String ownerName, String repoName, String branchName) throws IOException {
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

	@SuppressWarnings("unchecked")
	@NotNull
	public static List<GithubBranch> getBranches(String ownerName, String repoName) throws IOException {
		final HttpUrl url = HttpUrl.get("https://api.github.com/repos/%s/%s/branches".formatted(ownerName, repoName))
				.newBuilder()
				.addQueryParameter("page", "1")
				.addQueryParameter("per_page", "30")
				.build();

		try (Response response = HttpUtils.CLIENT.newCall(new Request.Builder()
						.url(url)
						.header("Accept", "applications/vnd.github.v3+json")
						.build())
				.execute()) {

			final String json = response.body().string();

			final List<GithubBranch> branchList = new ArrayList<>();

			final DataArray branches = DataArray.fromJson(json);
			for (int i = 0; i < branches.length(); i++) {
				final DataObject branchObject = branches.getObject(i);

				final String name = branchObject.getString("name");
				final String sha = branchObject.getObject("commit").getString("sha");

				branchList.add(new GithubBranch(name, sha));
			}

			return branchList;
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

	@SuppressWarnings("unchecked")
	static BuildStatus triggerBuild(String latestCommitHash) throws IOException {
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

	public static BuildStatus waitForBuild(String hash) throws IOException {
		BuildStatus buildStatus = BuildStatus.IN_PROGRESS;
		for (int i = 0; i < 3; i++) {
			while ((buildStatus = triggerBuild(hash)) == BuildStatus.IN_PROGRESS) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					throw new RemoteException("Got interrupted while waiting for a jitpack build to finish", e);
				}
			}

			//Retry up to 3 time if build status isn't OK, sometimes jitpack may shit itself i don't know
			if (buildStatus == BuildStatus.OK) {
				break;
			}
		}

		return buildStatus;
	}

	public static void downloadJitpackDocs(@NotNull ArtifactInfo artifactInfo, @NotNull Path targetPath) throws IOException {
		try (Response response = HttpUtils.CLIENT.newCall(new Request.Builder()
						.url("https://jitpack.io/%s/%s/%s/%s-%s-javadoc.jar".formatted(artifactInfo.groupId().replace('.', '/'),
								artifactInfo.artifactId(),
								artifactInfo.version(),
								artifactInfo.artifactId(),
								artifactInfo.version()))
						.build())
				.execute()) {
			final ResponseBody body = response.body();
			if (body == null) throw new IOException("Got no ResponseBody for " + response.request().url());

			if (!response.isSuccessful()) throw new IOException("Got an unsuccessful response from " + response.request().url() + ", code: " + response.code());

			Files.copy(body.byteStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public static void downloadMavenDocs(@NotNull ArtifactInfo artifactInfo, @NotNull Path targetPath) throws IOException {
		try (Response response = HttpUtils.CLIENT.newCall(new Request.Builder()
						.url(MAVEN_JAVADOC_FORMAT.formatted(artifactInfo.groupId().replace('.', '/'),
								artifactInfo.artifactId(),
								artifactInfo.version(),
								artifactInfo.artifactId(),
								artifactInfo.version()))
						.build())
				.execute()) {
			final ResponseBody body = response.body();
			if (body == null) throw new IOException("Got no ResponseBody for " + response.request().url());

			if (!response.isSuccessful()) throw new IOException("Got an unsuccessful response from " + response.request().url() + ", code: " + response.code());

			Files.copy(body.byteStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	static void extractZip(Path tempZip, Path targetDocsFolder) throws IOException {
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
	}
}
