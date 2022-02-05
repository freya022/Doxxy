package com.freya02.bot.versioning;

import com.freya02.bot.Main;
import com.freya02.bot.commands.slash.docs.CommonDocsHandlers;
import com.freya02.bot.docs.DocIndexMap;
import com.freya02.bot.versioning.github.GithubBranch;
import com.freya02.bot.versioning.github.GithubUtils;
import com.freya02.bot.versioning.jitpack.BuildStatus;
import com.freya02.bot.versioning.jitpack.JitpackUtils;
import com.freya02.bot.versioning.maven.MavenUtils;
import com.freya02.botcommands.api.BContext;
import com.freya02.botcommands.api.Logging;
import com.freya02.docs.DocSourceType;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
			final ArtifactInfo latestJDA5Version = VersionsUtils.retrieveLatestJDA5Version();

			final boolean changed = !latestJDA5Version.equals(this.latestJDA5Version);
			if (changed) {
				LOGGER.info("JDA 5 version updated, went from {} to {}", this.latestJDA5Version.version(), latestJDA5Version.version());

				LOGGER.info("Downloading JDA 5 javadocs");

				final Path tempZip = Files.createTempFile("JDA5Docs", ".zip");
				MavenUtils.downloadMavenDocs(latestJDA5Version, tempZip);

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
			final ArtifactInfo latestJDA4Version = VersionsUtils.retrieveLatestJDA4Version();

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
			final GithubBranch latestBranch = GithubUtils.getLatestBranch("freya022", "BotCommands");

			final ArtifactInfo jdaVersionFromBotCommands = VersionsUtils.retrieveJDAVersionFromBotCommands(latestBranch.branchName());

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
			final ArtifactInfo latestBotCommandsVersion = VersionsUtils.retrieveLatestBotCommandsVersion();

			final boolean changed = !latestBotCommandsVersion.equals(this.latestBotCommandsVersion);
			if (changed) {
				LOGGER.info("BotCommands version updated, went from {} to {}", this.latestBotCommandsVersion.version(), latestBotCommandsVersion.version());

				LOGGER.info("Downloading BC javadocs");

				final BuildStatus buildStatus = JitpackUtils.waitForBuild(latestBotCommandsVersion.version());

				if (buildStatus != BuildStatus.OK) {
					LOGGER.error("BC build status is not OK, status: {}", buildStatus);

					return false;
				}

				final Path tempZip = Files.createTempFile("BotCommandsDocs", ".zip");
				JitpackUtils.downloadJitpackDocs(latestBotCommandsVersion, tempZip);

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
