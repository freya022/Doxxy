package com.freya02.bot.versioning.supplier

import com.freya02.bot.utils.Utils
import com.freya02.bot.versioning.ArtifactInfo

//This could have been done with a kind of language-independent builder, with methods like "addRepository" or "addDependency"
// and giving out the resulting build script
object DependencySupplier {
    fun formatBC(
        buildToolType: BuildToolType,
        jdaVersionFromBotCommands: ArtifactInfo,
        latestBotCommands: ArtifactInfo
    ): String = Utils.readResource("/dependencies_scripts/${buildToolType.folderName}/BotCommands.txt")
        .format(
            jdaVersionFromBotCommands.groupId, jdaVersionFromBotCommands.artifactId, jdaVersionFromBotCommands.version,
            latestBotCommands.groupId, latestBotCommands.artifactId, latestBotCommands.version
        )

    fun formatBCJitpack(
        buildToolType: BuildToolType,
        jdaVersionFromBotCommands: ArtifactInfo,
        latestBotCommands: ArtifactInfo
    ): String = Utils.readResource("/dependencies_scripts/${buildToolType.folderName}/BotCommands_Jitpack.txt")
        .format(
            jdaVersionFromBotCommands.groupId, jdaVersionFromBotCommands.artifactId, jdaVersionFromBotCommands.version,
            latestBotCommands.groupId, latestBotCommands.artifactId, latestBotCommands.version
        )

    fun formatJDA5(buildToolType: BuildToolType, version: ArtifactInfo): String =
        Utils.readResource("/dependencies_scripts/${buildToolType.folderName}/JDA5.txt")
            .format(version.groupId, version.artifactId, version.version)

    fun formatJitpack(buildToolType: BuildToolType, version: ArtifactInfo): String =
        Utils.readResource("/dependencies_scripts/${buildToolType.folderName}/Jitpack.txt")
            .format(version.groupId, version.artifactId, version.version)

    fun formatJDA4(buildToolType: BuildToolType, version: ArtifactInfo): String =
        Utils.readResource("/dependencies_scripts/${buildToolType.folderName}/JDA4.txt")
            .format(version.groupId, version.artifactId, version.version)
}