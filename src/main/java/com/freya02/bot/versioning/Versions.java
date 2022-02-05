package com.freya02.bot.versioning;

import com.freya02.bot.Main;
import com.freya02.bot.commands.slash.docs.CommonDocsHandlers;
import com.freya02.bot.docs.BuildStatus;
import com.freya02.bot.docs.DocIndexMap;
import com.freya02.bot.utils.HttpUtils;
import com.freya02.botcommands.api.BContext;
import com.freya02.botcommands.api.Logging;
import com.freya02.docs.DocSourceType;
import net.dv8tion.jda.api.exceptions.ParsingException;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.freya02.bot.commands.slash.docs.CommonDocsHandlers.AUTOCOMPLETE_NAMES;

public class Versions {
	private static final Logger LOGGER = Logging.getLogger();

	private final Path lastKnownVersionsFolderPath = Main.BOT_FOLDER.resolve("last_versions");
	private final Path lastKnownBotCommandsPath = lastKnownVersionsFolderPath.resolve("BC.txt");
	private final Path lastKnownJDAFromBCPath = lastKnownVersionsFolderPath.resolve("JDA_from_BC.txt");
	private final Path lastKnownJDA4Path = lastKnownVersionsFolderPath.resolve("JDA4.txt");
	private final Path lastKnownJDA5Path = lastKnownVersionsFolderPath.resolve("JDA5.txt");

	private ArtifactInfo latestBotCommandsVersion;
	private ArtifactInfo jdaVersionFromBotCommands;
	private ArtifactInfo latestJDA4Version;
	private ArtifactInfo latestJDA5Version;

	public Versions() throws IOException {
		this.latestBotCommandsVersion = readLastKnownVersion(lastKnownBotCommandsPath);
		this.jdaVersionFromBotCommands = readLastKnownVersion(lastKnownJDAFromBCPath);
		this.latestJDA4Version = readLastKnownVersion(lastKnownJDA4Path);
		this.latestJDA5Version = readLastKnownVersion(lastKnownJDA5Path);

		Runtime.getRuntime().addShutdownHook(new Thread(this::saveLastKnownVersions));
	}

	private void saveLastKnownVersions() {
		try {
			Files.createDirectories(lastKnownVersionsFolderPath);

			Files.writeString(lastKnownBotCommandsPath, latestBotCommandsVersion.toFileString());
			Files.writeString(lastKnownJDAFromBCPath, jdaVersionFromBotCommands.toFileString());
			Files.writeString(lastKnownJDA4Path, latestJDA4Version.toFileString());
			Files.writeString(lastKnownJDA5Path, latestJDA5Version.toFileString());
		} catch (IOException e) {
			LOGGER.error("Unable to save last versions", e);
		}
	}

	private ArtifactInfo readLastKnownVersion(Path path) throws IOException {
		return ArtifactInfo.fromFileString(path);
	}

	public void initUpdateLoop(BContext context) throws IOException {
		final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

		//We need to check if the version has **not** changed between runs
		// If the version changed then it would have updated in the scheduled executor
		// But if the version didn't change then the docs wouldn't have been indexed
		// This is why we index them here, if no update is required
		if (!checkLatestBCVersion(context)) {
			//Load docs normally, version hasn't changed

			DocIndexMap.getInstance().get(DocSourceType.BOT_COMMANDS).reindex();
		}

		if (!checkLatestJDA5Version(context)) {
			//Load docs normally, version hasn't changed

			DocIndexMap.getInstance().get(DocSourceType.JDA).reindex();
		}

		//First index for Java's docs
		DocIndexMap.getInstance().get(DocSourceType.JAVA).reindex();

		//Once we loaded everything, invalidate caches if the user had time to use the commands before docs were loaded
		for (String autocompleteName : AUTOCOMPLETE_NAMES) {
			context.invalidateAutocompletionCache(autocompleteName);
		}

		scheduledExecutorService.scheduleWithFixedDelay(() -> checkLatestBCVersion(context), 30, 30, TimeUnit.MINUTES);
		scheduledExecutorService.scheduleWithFixedDelay(this::checkLatestJDAVersionFromBC, 0, 30, TimeUnit.MINUTES);
		scheduledExecutorService.scheduleWithFixedDelay(this::checkLatestJDA4Version, 0, 30, TimeUnit.MINUTES);
		scheduledExecutorService.scheduleWithFixedDelay(() -> checkLatestJDA5Version(context), 30, 30, TimeUnit.MINUTES);
	}

	private boolean checkLatestJDA5Version(BContext context) {
		try {
			final ArtifactInfo latestJDA5Version = retrieveLatestJDA5Version();

			final boolean changed = !latestJDA5Version.equals(this.latestJDA5Version);
			if (changed) {
				LOGGER.info("JDA 5 version updated, went from {} to {}", this.latestJDA5Version.version(), latestJDA5Version.version());

				LOGGER.info("Downloading JDA 5 javadocs");

				final Path tempZip = Files.createTempFile("JDA5Docs", ".zip");
				VersionsUtils.downloadMavenDocs(latestJDA5Version, tempZip);

				final Path targetDocsFolder = Main.JAVADOCS_PATH.resolve("JDA");

				VersionsUtils.extractZip(tempZip, targetDocsFolder);

				Files.deleteIfExists(tempZip);

				LOGGER.info("Downloaded JDA 5 javadocs");

				DocIndexMap.refreshAndInvalidateIndex(DocSourceType.JDA);

				for (String handlerName : CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
					context.invalidateAutocompletionCache(handlerName);
				}
			}

			this.latestJDA5Version = latestJDA5Version;

			return changed;
		} catch (IOException e) {
			LOGGER.error("An exception occurred while retrieving versions", e);
		}

		return false;
	}

	private void checkLatestJDA4Version() {
		try {
			final ArtifactInfo latestJDA4Version = retrieveLatestJDA4Version();

			if (!latestJDA4Version.equals(this.latestJDA4Version)) {
				LOGGER.info("JDA 4 version updated, went from {} to {}", this.latestJDA4Version.version(), latestJDA4Version.version());
			}

			this.latestJDA4Version = latestJDA4Version;
		} catch (IOException e) {
			LOGGER.error("An exception occurred while retrieving versions", e);
		}
	}

	private boolean checkLatestJDAVersionFromBC() {
		try {
			final GithubBranch latestBranch = getLatestBranch("freya022", "BotCommands");

			final ArtifactInfo jdaVersionFromBotCommands = retrieveJDAVersionFromBotCommands(latestBranch.branchName());

			final boolean changed = !jdaVersionFromBotCommands.equals(this.jdaVersionFromBotCommands);
			if (changed) {
				LOGGER.info("BotCommands's JDA version updated, went from {} to {}", this.jdaVersionFromBotCommands.version(), jdaVersionFromBotCommands.version());
			}

			this.jdaVersionFromBotCommands = jdaVersionFromBotCommands;

			return changed;
		} catch (IOException e) {
			LOGGER.error("An exception occurred while retrieving versions", e);
		}

		return false;
	}

	private boolean checkLatestBCVersion(BContext context) {
		try {
			final ArtifactInfo latestBotCommandsVersion = retrieveLatestBotCommandsVersion();

			final boolean changed = !latestBotCommandsVersion.equals(this.latestBotCommandsVersion);
			if (changed) {
				LOGGER.info("BotCommands version updated, went from {} to {}", this.latestBotCommandsVersion.version(), latestBotCommandsVersion.version());

				LOGGER.info("Downloading BC javadocs");

				final BuildStatus buildStatus = VersionsUtils.waitForBuild(latestBotCommandsVersion.version());

				if (buildStatus != BuildStatus.OK) {
					LOGGER.error("BC build status is not OK, status: {}", buildStatus);

					return false;
				}

				final Path tempZip = Files.createTempFile("BotCommandsDocs", ".zip");
				VersionsUtils.downloadJitpackDocs(latestBotCommandsVersion, tempZip);

				final Path targetDocsFolder = Main.JAVADOCS_PATH.resolve("BotCommands");

				VersionsUtils.extractZip(tempZip, targetDocsFolder);

				Files.deleteIfExists(tempZip);

				LOGGER.info("Downloaded BC javadocs");

				DocIndexMap.refreshAndInvalidateIndex(DocSourceType.BOT_COMMANDS);

				for (String handlerName : CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
					context.invalidateAutocompletionCache(handlerName);
				}
			}

			this.latestBotCommandsVersion = latestBotCommandsVersion;

			return changed;
		} catch (IOException e) {
			LOGGER.error("An exception occurred while retrieving versions", e);
		}

		return false;
	}

	@NotNull
	private ArtifactInfo retrieveJDAVersionFromBotCommands(String branchName) throws IOException {
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
	private ArtifactInfo retrieveLatestBotCommandsVersion() throws IOException {
		final String ownerName = "freya022";
		final String groupId = "com.github." + ownerName;
		final String artifactId = "BotCommands";

		final GithubBranch latestBranch = getLatestBranch(ownerName, artifactId);

		return new ArtifactInfo(groupId,
				artifactId,
				latestBranch.latestCommitSha10());
	}

	@NotNull
	public static GithubBranch getLatestBranch(String ownerName, String artifactId) throws IOException {
		final List<GithubBranch> branches = VersionsUtils.getBranches(ownerName, artifactId);

		return branches.stream()
				.filter(s -> s.branchName().matches("\\d\\.\\d\\.\\d"))
				.max(Comparator.comparing(GithubBranch::branchName))
				.orElseThrow();
	}

	@NotNull
	private ArtifactInfo retrieveLatestJDA4Version() throws IOException {
		final String groupId = "net.dv8tion";
		final String artifactId = "JDA";

		return new ArtifactInfo(groupId,
				artifactId,
				VersionsUtils.getLatestMavenVersion(VersionsUtils.M2_METADATA_FORMAT, groupId, artifactId));
	}

	@NotNull
	private ArtifactInfo retrieveLatestJDA5Version() throws IOException {
		final String groupId = "net.dv8tion";
		final String artifactId = "JDA";

		return new ArtifactInfo(groupId,
				artifactId,
				VersionsUtils.getLatestMavenVersion(VersionsUtils.MAVEN_METADATA_FORMAT, groupId, artifactId));
	}

	public ArtifactInfo getLatestBotCommandsVersion() {
		return latestBotCommandsVersion;
	}

	public ArtifactInfo getJdaVersionFromBotCommands() {
		return jdaVersionFromBotCommands;
	}

	public ArtifactInfo getLatestJDA4Version() {
		return latestJDA4Version;
	}

	public ArtifactInfo getLatestJDA5Version() {
		return latestJDA5Version;
	}
}
