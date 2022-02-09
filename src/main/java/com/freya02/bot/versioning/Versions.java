package com.freya02.bot.versioning;

import com.freya02.bot.Main;
import com.freya02.bot.commands.slash.docs.CommonDocsHandlers;
import com.freya02.bot.docs.DocIndexMap;
import com.freya02.bot.utils.ProcessUtils;
import com.freya02.bot.versioning.github.GithubBranch;
import com.freya02.bot.versioning.jitpack.JitpackUtils;
import com.freya02.bot.versioning.jitpack.JitpackVersionChecker;
import com.freya02.bot.versioning.maven.MavenProjectDependencyVersionChecker;
import com.freya02.bot.versioning.maven.MavenUtils;
import com.freya02.bot.versioning.maven.MavenVersionChecker;
import com.freya02.bot.versioning.maven.RepoType;
import com.freya02.botcommands.api.BContext;
import com.freya02.botcommands.api.Logging;
import com.freya02.docs.DocSourceType;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.freya02.bot.commands.slash.docs.CommonDocsHandlers.AUTOCOMPLETE_NAMES;

public class Versions {
	private static final Logger LOGGER = Logging.getLogger();

	private static final Path lastKnownVersionsFolderPath = Main.BOT_FOLDER.resolve("last_versions");
	private static final Path lastKnownBotCommandsPath = lastKnownVersionsFolderPath.resolve("BC.txt");
	private static final Path lastKnownJDAFromBCPath = lastKnownVersionsFolderPath.resolve("JDA_from_BC.txt");
	private static final Path lastKnownJDA4Path = lastKnownVersionsFolderPath.resolve("JDA4.txt");
	private static final Path lastKnownJDA5Path = lastKnownVersionsFolderPath.resolve("JDA5.txt");

	private static final Path JDA_DOCS_FOLDER = Main.JAVADOCS_PATH.resolve("JDA");
	private static final Path BC_DOCS_FOLDER = Main.JAVADOCS_PATH.resolve("BotCommands");

	private final JitpackVersionChecker bcChecker;
	private final MavenProjectDependencyVersionChecker jdaVersionFromBCChecker;
	private final MavenVersionChecker jda4Checker;
	private final MavenVersionChecker jda5Checker;

	public Versions() throws IOException {
		Files.createDirectories(lastKnownVersionsFolderPath);

		this.bcChecker = new JitpackVersionChecker(lastKnownBotCommandsPath, "freya022", "com.github.freya022", "BotCommands");
		this.jdaVersionFromBCChecker = new MavenProjectDependencyVersionChecker(lastKnownJDAFromBCPath, "freya022", "BotCommands", "JDA");
		this.jda4Checker = new MavenVersionChecker(lastKnownJDA4Path, RepoType.M2, "net.dv8tion", "JDA");
		this.jda5Checker = new MavenVersionChecker(lastKnownJDA5Path, RepoType.MAVEN, "net.dv8tion", "JDA");
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

		scheduledExecutorService.scheduleWithFixedDelay(() -> checkLatestBCVersion(context), 30, 30, TimeUnit.MINUTES);
		scheduledExecutorService.scheduleWithFixedDelay(this::checkLatestJDAVersionFromBC, 0, 30, TimeUnit.MINUTES);
		scheduledExecutorService.scheduleWithFixedDelay(this::checkLatestJDA4Version, 0, 30, TimeUnit.MINUTES);
		scheduledExecutorService.scheduleWithFixedDelay(() -> checkLatestJDA5Version(context), 30, 30, TimeUnit.MINUTES);

		//First index for Java's docs, may take some time
		DocIndexMap.getInstance().get(DocSourceType.JAVA).reindex();

		//Once we loaded everything, invalidate caches if the user had time to use the commands before docs were loaded
		for (String autocompleteName : AUTOCOMPLETE_NAMES) {
			context.invalidateAutocompletionCache(autocompleteName);
		}
	}

	private boolean checkLatestJDA5Version(BContext context) {
		try {
			final boolean changed = jda5Checker.checkVersion();

			if (changed) {
				LOGGER.info("JDA 5 version changed");

				LOGGER.debug("Downloading JDA 5 javadocs");

				final Path tempZip = Files.createTempFile("JDA5Docs", ".zip");
				MavenUtils.downloadMavenDocs(jda5Checker.getLatest(), tempZip);

				LOGGER.debug("Extracting JDA 5 javadocs");
				VersionsUtils.extractZip(tempZip, JDA_DOCS_FOLDER);

				Files.deleteIfExists(tempZip);

				LOGGER.debug("Invalidating JDA 5 index");
				DocIndexMap.refreshAndInvalidateIndex(DocSourceType.JDA);

				for (String handlerName : CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
					context.invalidateAutocompletionCache(handlerName);
				}

				jda5Checker.saveVersion();

				LOGGER.info("JDA 5 version updated to {}", jda5Checker.getLatest().version());
			}

			return changed;
		} catch (IOException e) {
			LOGGER.error("An exception occurred while retrieving versions", e);
		}

		return false;
	}

	private void checkLatestJDA4Version() {
		try {
			final boolean changed = jda4Checker.checkVersion();

			if (changed) {
				LOGGER.info("JDA 4 version changed");

				jda4Checker.saveVersion();

				LOGGER.info("JDA 4 version updated to {}", jda4Checker.getLatest().version());
			}
		} catch (IOException e) {
			LOGGER.error("An exception occurred while retrieving versions", e);
		}
	}

	private void checkLatestJDAVersionFromBC() {
		try {
			final boolean changed = jdaVersionFromBCChecker.checkVersion();

			if (changed) {
				LOGGER.info("BotCommands's JDA version changed");

				jdaVersionFromBCChecker.saveVersion();

				LOGGER.info("BotCommands's JDA version updated to {}", jdaVersionFromBCChecker.getLatest().version());
			}
		} catch (IOException e) {
			LOGGER.error("An exception occurred while retrieving versions", e);
		}
	}

	private boolean checkLatestBCVersion(BContext context) {
		try {
			final boolean changed = bcChecker.checkVersion();

			if (changed) {
				LOGGER.info("BotCommands version changed");

				final Path BCRepoPath = Main.REPOS_PATH.resolve("BotCommands");
				final boolean needClone = Files.notExists(BCRepoPath);
				if (needClone) {
					LOGGER.debug("Cloning BC repo");

					ProcessUtils.runAndWait("git clone https://github.com/freya022/BotCommands.git", Main.REPOS_PATH);
				} else {
					LOGGER.debug("Fetching BC repo");

					ProcessUtils.runAndWait("git fetch", BCRepoPath);
				}

				final GithubBranch latestBranch = bcChecker.getLatestBranch();

				LOGGER.debug("Switching to BC branch {}", latestBranch.branchName());
				ProcessUtils.runAndWait("git checkout " + latestBranch.branchName(), BCRepoPath);

				if (!needClone) {
					LOGGER.debug("Pulling changes from BC branch {}", latestBranch.branchName());
					ProcessUtils.runAndWait("git pull", BCRepoPath);
				}

				LOGGER.debug("Running mvn javadoc:javadoc");
				if (System.getProperty("os.name").toLowerCase().contains("windows")) {
					ProcessUtils.runAndWait("mvn.cmd javadoc:javadoc", BCRepoPath);
				} else {
					ProcessUtils.runAndWait("mvn javadoc:javadoc", BCRepoPath);
				}

				final Path targetDocsFolder = BC_DOCS_FOLDER;

				if (Files.exists(targetDocsFolder)) {
					LOGGER.debug("Removing old BC docs at {}", targetDocsFolder);

					for (Path path : Files.walk(targetDocsFolder).sorted(Comparator.reverseOrder()).toList()) {
						Files.deleteIfExists(path);
					}
				}

				final Path apiDocsPath = BCRepoPath.resolve("target").resolve("site").resolve("apidocs");
				LOGGER.debug("Moving docs from {} to {}", apiDocsPath, targetDocsFolder);
				for (Path sourcePath : Files.walk(apiDocsPath)
						.filter(Files::isRegularFile)
						.filter(p -> p.getFileName().toString().endsWith("html"))
						.toList()) {
					final Path targetPath = targetDocsFolder.resolve(apiDocsPath.relativize(sourcePath).toString());

					Files.createDirectories(targetPath.getParent());
					Files.move(sourcePath, targetPath, StandardCopyOption.ATOMIC_MOVE);
				}

				LOGGER.debug("Invalidating BotCommands index");
				DocIndexMap.refreshAndInvalidateIndex(DocSourceType.BOT_COMMANDS);

				for (String handlerName : CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
					context.invalidateAutocompletionCache(handlerName);
				}

				bcChecker.saveVersion();

				LOGGER.info("BotCommands version updated to {}", bcChecker.getLatest().version());

				JitpackUtils.triggerBuild(bcChecker.getLatest().groupId(), bcChecker.getLatest().artifactId(), bcChecker.getLatest().version());
			}

			return changed;
		} catch (IOException | InterruptedException e) {
			LOGGER.error("An exception occurred while retrieving versions", e);
		}

		return false;
	}

	public ArtifactInfo getLatestBotCommandsVersion() {
		return bcChecker.getLatest();
	}

	public ArtifactInfo getJdaVersionFromBotCommands() {
		return jdaVersionFromBCChecker.getLatest();
	}

	public ArtifactInfo getLatestJDA4Version() {
		return jda4Checker.getLatest();
	}

	public ArtifactInfo getLatestJDA5Version() {
		return jda5Checker.getLatest();
	}
}
