package com.freya02.bot.versioning.supplier

import com.freya02.bot.versioning.ArtifactInfo
import com.freya02.bot.versioning.ScriptType

//This could have been done with a kind of language-independent builder, with methods like "addRepository" or "addDependency"
// and giving out the resulting build script
object DependencySupplier {
    @Throws(UnsupportedDependencyException::class)
    fun formatBC(
        scriptType: ScriptType,
        buildToolType: BuildToolType,
        jdaVersionFromBotCommands: ArtifactInfo,
        latestBotCommands: ArtifactInfo
    ): String = readResource("/${scriptType.folderName}/${buildToolType.folderName}/BotCommands.${buildToolType.getEffectiveExtension(scriptType)}")
        .format(
            jdaVersionFromBotCommands.groupId, jdaVersionFromBotCommands.artifactId, jdaVersionFromBotCommands.version,
            latestBotCommands.groupId, latestBotCommands.artifactId, latestBotCommands.version
        )

    @Throws(UnsupportedDependencyException::class)
    fun formatBCJitpack(
        scriptType: ScriptType,
        buildToolType: BuildToolType,
        jdaVersionFromBotCommands: ArtifactInfo,
        latestBotCommands: ArtifactInfo
    ): String = readResource("/${scriptType.folderName}/${buildToolType.folderName}/BotCommands_Jitpack.${buildToolType.getEffectiveExtension(scriptType)}")
        .format(
            jdaVersionFromBotCommands.groupId, jdaVersionFromBotCommands.artifactId, jdaVersionFromBotCommands.version,
            latestBotCommands.groupId, latestBotCommands.artifactId, latestBotCommands.version
        )

    @Throws(UnsupportedDependencyException::class)
    fun formatJDA5(scriptType: ScriptType, buildToolType: BuildToolType, version: ArtifactInfo): String =
        readResource("/${scriptType.folderName}/${buildToolType.folderName}/JDA5.${buildToolType.getEffectiveExtension(scriptType)}")
            .format(version.groupId, version.artifactId, version.version)

    @Throws(UnsupportedDependencyException::class)
    fun formatJitpack(scriptType: ScriptType, buildToolType: BuildToolType, version: ArtifactInfo): String =
        readResource("/${scriptType.folderName}/${buildToolType.folderName}/Jitpack.${buildToolType.getEffectiveExtension(scriptType)}")
            .format(version.groupId, version.artifactId, version.version)

    @Throws(UnsupportedDependencyException::class)
    fun formatJDA4(scriptType: ScriptType, buildToolType: BuildToolType, version: ArtifactInfo): String =
        readResource("/${scriptType.folderName}/${buildToolType.folderName}/JDA4.${buildToolType.getEffectiveExtension(scriptType)}")
            .format(version.groupId, version.artifactId, version.version)

    private fun BuildToolType.getEffectiveExtension(scriptType: ScriptType): String = when (scriptType) {
        ScriptType.DEPENDENCIES -> "txt"
        ScriptType.FULL -> this.fileExtension
    }

    @Throws(UnsupportedDependencyException::class)
    private fun readResource(path: String): String {
        val stream = DependencySupplier::class.java.getResourceAsStream(path)
            ?: throw UnsupportedDependencyException("Unable to find the DependencySupplier resource: $path")
        return stream.readAllBytes().decodeToString()
    }
}