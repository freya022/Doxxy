package com.freya02.bot.versioning;

import com.freya02.bot.utils.HttpUtils;
import com.freya02.bot.versioning.github.GithubBranch;
import com.freya02.bot.versioning.github.GithubUtils;
import com.freya02.bot.versioning.maven.MavenUtils;
import net.dv8tion.jda.api.exceptions.ParsingException;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;

public class VersionsUtils {


	public static void extractZip(Path tempZip, Path targetDocsFolder) throws IOException {
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

	@NotNull
	public static ArtifactInfo retrieveJDAVersionFromBotCommands(String branchName) throws IOException {
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
	public static ArtifactInfo retrieveLatestBotCommandsVersion() throws IOException {
		final String ownerName = "freya022";
		final String groupId = "com.github." + ownerName;
		final String artifactId = "BotCommands";

		final GithubBranch latestBranch = GithubUtils.getLatestBranch(ownerName, artifactId);

		return new ArtifactInfo(groupId,
				artifactId,
				latestBranch.latestCommitSha10());
	}

	@NotNull
	public static ArtifactInfo retrieveLatestJDA4Version() throws IOException {
		final String groupId = "net.dv8tion";
		final String artifactId = "JDA";

		return new ArtifactInfo(groupId,
				artifactId,
				MavenUtils.getLatestMavenVersion(MavenUtils.M2_METADATA_FORMAT, groupId, artifactId));
	}

	@NotNull
	public static ArtifactInfo retrieveLatestJDA5Version() throws IOException {
		final String groupId = "net.dv8tion";
		final String artifactId = "JDA";

		return new ArtifactInfo(groupId,
				artifactId,
				MavenUtils.getLatestMavenVersion(MavenUtils.MAVEN_METADATA_FORMAT, groupId, artifactId));
	}
}
