package com.freya02.bot.versioning;

import com.freya02.bot.docs.DocIndexMap;
import com.freya02.bot.utils.HttpUtils;
import com.freya02.botcommands.api.Logging;
import com.freya02.docs.DocSourceType;
import net.dv8tion.jda.api.exceptions.ParsingException;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Versions {
	private static final Logger LOGGER = Logging.getLogger();

	private ArtifactInfo latestBotCommandsVersion;
	private ArtifactInfo jdaVersionFromBotCommands;
	private ArtifactInfo latestJDA4Version;
	private ArtifactInfo latestJDA5Version;

	public Versions() {
		Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
			try {
				final ArtifactInfo latestBotCommandsVersion = retrieveLatestBotCommandsVersion("2.3.0");
				final ArtifactInfo jdaVersionFromBotCommands = retrieveJDAVersionFromBotCommands("2.3.0");
				final ArtifactInfo latestJDA4Version = retrieveLatestJDA4Version();
				final ArtifactInfo latestJDA5Version = retrieveLatestJDA5Version();

				if (!latestBotCommandsVersion.equals(this.latestBotCommandsVersion)) {
					DocIndexMap.refreshIndex(DocSourceType.BOT_COMMANDS);

					//TODO invalidate autocomplete
				} else if (!jdaVersionFromBotCommands.equals(this.jdaVersionFromBotCommands)) {
					DocIndexMap.refreshIndex(DocSourceType.JDA);

					//TODO invalidate autocomplete
				} else if (!latestJDA4Version.equals(this.latestJDA4Version)) {
					//TODO invalidate autocomplete
				} else if (!latestJDA5Version.equals(this.latestJDA5Version)) {
					//TODO invalidate autocomplete
				}

				this.latestBotCommandsVersion = latestBotCommandsVersion;
				this.jdaVersionFromBotCommands = jdaVersionFromBotCommands;
				this.latestJDA4Version = latestJDA4Version;
				this.latestJDA5Version = latestJDA5Version;
			} catch (IOException e) {
				LOGGER.error("An exception occurred while retrieving versions", e);
			}
		}, 0, 30, TimeUnit.MINUTES);
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
