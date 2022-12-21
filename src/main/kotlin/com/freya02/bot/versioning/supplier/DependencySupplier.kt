package com.freya02.bot.versioning.supplier

import com.freya02.bot.utils.Utils
import com.freya02.bot.versioning.ArtifactInfo
import com.freya02.bot.versioning.ScriptType

//This could have been done with a kind of language-independent builder, with methods like "addRepository" or "addDependency"
// and giving out the resulting build script
object DependencySupplier {
    fun formatBC(
        scriptType: ScriptType,
        buildToolType: BuildToolType,
        jdaVersionFromBotCommands: ArtifactInfo,
        latestBotCommands: ArtifactInfo
    ): String = Utils.readResource("/${scriptType.folderName}/${buildToolType.folderName}/BotCommands.txt")
        .format(
            jdaVersionFromBotCommands.groupId, jdaVersionFromBotCommands.artifactId, jdaVersionFromBotCommands.version,
            latestBotCommands.groupId, latestBotCommands.artifactId, latestBotCommands.version
        )

    fun formatBCJitpack(
        scriptType: ScriptType,
        buildToolType: BuildToolType,
        jdaVersionFromBotCommands: ArtifactInfo,
        latestBotCommands: ArtifactInfo
    ): String = Utils.readResource("/${scriptType.folderName}/${buildToolType.folderName}/BotCommands_Jitpack.txt")
        .format(
            jdaVersionFromBotCommands.groupId, jdaVersionFromBotCommands.artifactId, jdaVersionFromBotCommands.version,
            latestBotCommands.groupId, latestBotCommands.artifactId, latestBotCommands.version
        )

    fun formatJDA5(scriptType: ScriptType, buildToolType: BuildToolType, version: ArtifactInfo): String =
        Utils.readResource("/${scriptType.folderName}/${buildToolType.folderName}/JDA5.txt")
            .format(version.groupId, version.artifactId, version.version)

    fun formatJitpack(scriptType: ScriptType, buildToolType: BuildToolType, version: ArtifactInfo): String =
        Utils.readResource("/${scriptType.folderName}/${buildToolType.folderName}/Jitpack.txt")
            .format(version.groupId, version.artifactId, version.version)

    fun formatJDA4(scriptType: ScriptType, buildToolType: BuildToolType, version: ArtifactInfo): String =
        Utils.readResource("/${scriptType.folderName}/${buildToolType.folderName}/JDA4.txt")
            .format(version.groupId, version.artifactId, version.version)
}