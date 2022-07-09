package com.freya02.bot.versioning.supplier

import com.freya02.bot.versioning.ArtifactInfo

object DependencySupplier {
    private val dependenciesMap = mapOf(
        BuildToolType.MAVEN to MavenDependencies,
        BuildToolType.GRADLE to GradleDependencies,
        BuildToolType.GRADLE_KTS to KotlinGradleDependencies
    )

    fun formatBC(
        buildToolType: BuildToolType,
        jdaVersionFromBotCommands: ArtifactInfo,
        latestBotCommands: ArtifactInfo
    ): String {
        return dependenciesMap[buildToolType]!!
            .bcDependencyFormatString
            .format(
                jdaVersionFromBotCommands.groupId,
                jdaVersionFromBotCommands.artifactId,
                jdaVersionFromBotCommands.version,
                latestBotCommands.groupId,
                latestBotCommands.artifactId,
                latestBotCommands.version
            )
    }

    fun formatJDA5(buildToolType: BuildToolType, version: ArtifactInfo): String {
        return dependenciesMap[buildToolType]!!
            .jda5DependencyFormatString
            .format(version.groupId, version.artifactId, version.version)
    }

    fun formatJDA5Jitpack(buildToolType: BuildToolType, version: ArtifactInfo): String {
        return dependenciesMap[buildToolType]!!
            .jdaJitpackDependencyFormatString
            .format(version.groupId, version.artifactId, version.version)
    }

    fun formatJDA4(buildToolType: BuildToolType, version: ArtifactInfo): String {
        return dependenciesMap[buildToolType]!!
            .jda4DependencyFormatString
            .format(version.groupId, version.artifactId, version.version)
    }
}