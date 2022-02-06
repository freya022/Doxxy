package com.freya02.bot.versioning.maven;

import com.freya02.bot.utils.HttpUtils;
import com.freya02.bot.versioning.ArtifactInfo;
import com.freya02.bot.versioning.VersionChecker;
import com.freya02.bot.versioning.github.GithubBranch;
import com.freya02.bot.versioning.github.GithubUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class MavenProjectDependencyVersionChecker extends VersionChecker {
	private final String ownerName;
	private final String artifactId;

	private final String targetArtifactId;

	public MavenProjectDependencyVersionChecker(Path lastSavedPath, String ownerName, String artifactId, String targetArtifactId) throws IOException {
		super(lastSavedPath);

		this.ownerName = ownerName;
		this.artifactId = artifactId;

		this.targetArtifactId = targetArtifactId;
	}

	@Override
	public boolean checkVersion() throws IOException {
		final GithubBranch latestBranch = GithubUtils.getLatestBranch(ownerName, artifactId);

		final ArtifactInfo latestDependencyVersion = retrieveDependencyVersion(latestBranch.branchName());

		final boolean changed = !latestDependencyVersion.equals(this.latest);

		this.latest = latestDependencyVersion;

		return changed;
	}

	@NotNull
	private ArtifactInfo retrieveDependencyVersion(String branchName) throws IOException {
		final Document document = HttpUtils.getDocument("https://raw.githubusercontent.com/%s/%s/%s/pom.xml".formatted(this.ownerName, artifactId, branchName));

		Elements elements = document.select("project > dependencies > dependency");
		for (int i = 0, selectSize = elements.size(); i < selectSize; i++) {
			final Element element = elements.get(i);

			final Element groupIdElement = Objects.requireNonNull(element.selectFirst("groupId"), "Could not parse dependency #" + i + " at " + document.baseUri());
			final Element artifactIdElement = Objects.requireNonNull(element.selectFirst("artifactId"), "Could not parse dependency #" + i + " at " + document.baseUri());
			final Element versionElement = Objects.requireNonNull(element.selectFirst("version"), "Could not parse dependency #" + i + " at " + document.baseUri());

			if (!artifactIdElement.text().equals(targetArtifactId)) continue;

			return new ArtifactInfo(
					groupIdElement.text(),
					artifactIdElement.text(),
					versionElement.text()
			);
		}

		throw new IOException("Unable to get dependency version from " + document.baseUri());
	}
}
