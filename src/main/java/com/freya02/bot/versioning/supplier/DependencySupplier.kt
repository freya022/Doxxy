package com.freya02.bot.versioning.supplier;

import com.freya02.bot.versioning.ArtifactInfo;

import java.util.Map;

public class DependencySupplier {
	private static final Map<BuildToolType, BuildToolDependencies> dependenciesMap = Map.of(
			BuildToolType.MAVEN, new MavenDependencies(),
			BuildToolType.GRADLE, new GradleDependencies(),
			BuildToolType.GRADLE_KTS, new KotlinGradleDependencies()
	);

	public static String formatBC(BuildToolType buildToolType, ArtifactInfo jdaVersionFromBotCommands, ArtifactInfo latestBotCommands) {
		return dependenciesMap.get(buildToolType)
				.getBCDependencyFormatString()
				.formatted(jdaVersionFromBotCommands.getGroupId(), jdaVersionFromBotCommands.getArtifactId(), jdaVersionFromBotCommands.getVersion(),
				latestBotCommands.getGroupId(), latestBotCommands.getArtifactId(), latestBotCommands.getVersion());
	}

	public static String formatJDA5(BuildToolType buildToolType, ArtifactInfo version) {
		return dependenciesMap.get(buildToolType)
				.getJDA5DependencyFormatString()
				.formatted(version.getGroupId(), version.getArtifactId(), version.getVersion());
	}

	public static String formatJDA5Jitpack(BuildToolType buildToolType, ArtifactInfo version) {
		return dependenciesMap.get(buildToolType)
				.getJDA5JitpackDependencyFormatString()
				.formatted(version.getGroupId(), version.getArtifactId(), version.getVersion());
	}

	public static String formatJDA4(BuildToolType buildToolType, ArtifactInfo version) {
		return dependenciesMap.get(buildToolType)
				.getJDA4DependencyFormatString()
				.formatted(version.getGroupId(), version.getArtifactId(), version.getVersion());
	}
}
