package com.freya02.bot.versioning;

import com.freya02.bot.commands.slash.docs.CommonDocsHandlers;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Versions {
	private static final Logger LOGGER = Logging.getLogger();

	private ArtifactInfo latestBotCommandsVersion;
	private ArtifactInfo jdaVersionFromBotCommands;
	private ArtifactInfo latestJDA4Version;
	private ArtifactInfo latestJDA5Version;

	public Versions() throws IOException {
		this.latestBotCommandsVersion = retrieveLatestBotCommandsVersion("2.3.0");
		this.jdaVersionFromBotCommands = retrieveJDAVersionFromBotCommands("2.3.0");
		this.latestJDA4Version = retrieveLatestJDA4Version();
		this.latestJDA5Version = retrieveLatestJDA5Version();
	}

	public void initUpdateLoop(BContext context) {
		final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

		scheduledExecutorService.scheduleWithFixedDelay(() -> checkLatestBCVersion(context), 30, 30, TimeUnit.MINUTES);
		scheduledExecutorService.scheduleWithFixedDelay(() -> checkLatestJDAVersionFromBC(context), 30, 30, TimeUnit.MINUTES);
		scheduledExecutorService.scheduleWithFixedDelay(this::checkLatestJDA4Version, 30, 30, TimeUnit.MINUTES);
		scheduledExecutorService.scheduleWithFixedDelay(this::checkLatestJDA5Version, 30, 30, TimeUnit.MINUTES);
	}

	private void checkLatestJDA5Version() {
		try {
			final ArtifactInfo latestJDA5Version = retrieveLatestJDA5Version();

			if (!latestJDA5Version.equals(this.latestJDA5Version)) {
				LOGGER.info("JDA 5 version updated, went from {} to {}", this.latestJDA5Version.version(), latestJDA5Version.version());
			}

			this.latestJDA5Version = latestJDA5Version;
		} catch (IOException e) {
			LOGGER.error("An exception occurred while retrieving versions", e);
		}
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

	private void checkLatestJDAVersionFromBC(BContext context) {
		try {
			final ArtifactInfo jdaVersionFromBotCommands = retrieveJDAVersionFromBotCommands("2.3.0");

			if (!jdaVersionFromBotCommands.equals(this.jdaVersionFromBotCommands)) {
				LOGGER.info("BotCommands's JDA version updated, went from {} to {}", this.jdaVersionFromBotCommands.version(), jdaVersionFromBotCommands.version());

				DocIndexMap.refreshAndInvalidateIndex(DocSourceType.JDA);

				for (String handlerName : CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
					context.invalidateAutocompletionCache(handlerName);
				}
			}

			this.jdaVersionFromBotCommands = jdaVersionFromBotCommands;
		} catch (IOException e) {
			LOGGER.error("An exception occurred while retrieving versions", e);
		}
	}

	private void checkLatestBCVersion(BContext context) {
		try {
			final ArtifactInfo latestBotCommandsVersion = retrieveLatestBotCommandsVersion("2.3.0");

			if (!latestBotCommandsVersion.equals(this.latestBotCommandsVersion)) {
				LOGGER.info("BotCommands version updated, went from {} to {}", this.latestBotCommandsVersion.version(), latestBotCommandsVersion.version());

				DocIndexMap.refreshAndInvalidateIndex(DocSourceType.BOT_COMMANDS);

				for (String handlerName : CommonDocsHandlers.AUTOCOMPLETE_NAMES) {
					context.invalidateAutocompletionCache(handlerName);
				}
			}

			this.latestBotCommandsVersion = latestBotCommandsVersion;
		} catch (IOException e) {
			LOGGER.error("An exception occurred while retrieving versions", e);
		}
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
	private ArtifactInfo retrieveLatestBotCommandsVersion(String branchName) throws IOException {
		final String ownerName = "freya022";
		final String groupId = "com.github." + ownerName;
		final String artifactId = "BotCommands";

		return new ArtifactInfo(groupId,
				artifactId,
				VersionsUtils.getLatestHash(ownerName, artifactId, branchName));
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
