package com.freya02.bot.versioning;

import com.freya02.bot.utils.HttpUtils;
import com.freya02.botcommands.api.Logging;
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
	private ArtifactInfo latestJDAVersion;

	public Versions() {
		Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
			try {
				latestBotCommandsVersion = retrieveLatestBotCommandsVersion("2.3.0");
				jdaVersionFromBotCommands = retrieveJDAVersionFromBotCommands("2.3.0");
				latestJDAVersion = retrieveLatestJDAVersion();
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
	private ArtifactInfo retrieveLatestJDAVersion() throws IOException {
		final String groupId = "net.dv8tion";
		final String artifactId = "JDA";

		return new ArtifactInfo(groupId,
				artifactId,
				VersionsUtils.getLatestMavenVersion(groupId, artifactId));
	}

	public ArtifactInfo getLatestBotCommandsVersion() {
		return latestBotCommandsVersion;
	}

	public ArtifactInfo getJdaVersionFromBotCommands() {
		return jdaVersionFromBotCommands;
	}

	public ArtifactInfo getLatestJDAVersion() {
		return latestJDAVersion;
	}
}
