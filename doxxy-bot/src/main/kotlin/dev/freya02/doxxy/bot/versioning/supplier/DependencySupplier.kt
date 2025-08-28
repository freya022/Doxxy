package dev.freya02.doxxy.bot.versioning.supplier

import dev.freya02.doxxy.bot.versioning.ArtifactInfo
import dev.freya02.doxxy.bot.versioning.ScriptType

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
        latestBotCommands: ArtifactInfo
    ): String = readResource("/${scriptType.folderName}/${buildToolType.folderName}/BotCommands_Jitpack.${buildToolType.getEffectiveExtension(scriptType)}")
        .format(
            latestBotCommands.groupId, latestBotCommands.artifactId, latestBotCommands.version
        )

    @Throws(UnsupportedDependencyException::class)
    fun formatJDA(scriptType: ScriptType, buildToolType: BuildToolType, version: ArtifactInfo): String =
        readResource("/${scriptType.folderName}/${buildToolType.folderName}/JDA.${buildToolType.getEffectiveExtension(scriptType)}")
            .format(version.groupId, version.artifactId, version.version)

    @Throws(UnsupportedDependencyException::class)
    fun formatJitpack(scriptType: ScriptType, buildToolType: BuildToolType, version: ArtifactInfo): String =
        readResource("/${scriptType.folderName}/${buildToolType.folderName}/Jitpack.${buildToolType.getEffectiveExtension(scriptType)}")
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
