package dev.freya02.doxxy.bot.versioning.supplier

import dev.freya02.doxxy.bot.versioning.ArtifactInfo
import dev.freya02.doxxy.bot.versioning.ScriptType

interface BuildScriptSupplier {

    @Throws(UnsupportedDependencyException::class)
    fun formatBC(
        buildToolType: BuildToolType,
        jdaVersionFromBotCommands: ArtifactInfo,
        latestBotCommands: ArtifactInfo
    ): String

    @Throws(UnsupportedDependencyException::class)
    fun formatBCJitpack(
        buildToolType: BuildToolType,
        latestBotCommands: ArtifactInfo
    ): String

    @Throws(UnsupportedDependencyException::class)
    fun formatJDA(buildToolType: BuildToolType, version: ArtifactInfo): String

    @Throws(UnsupportedDependencyException::class)
    fun formatJitpack(buildToolType: BuildToolType, version: ArtifactInfo): String

    object Full : BuildScriptSupplier {
        override fun formatBC(
            buildToolType: BuildToolType,
            jdaVersionFromBotCommands: ArtifactInfo,
            latestBotCommands: ArtifactInfo
        ): String {
            val path = "build_scripts/${buildToolType.folderName}/BotCommands.${buildToolType.fileExtension}"
            return readResource(path)
                .format(
                    jdaVersionFromBotCommands.groupId, jdaVersionFromBotCommands.artifactId, jdaVersionFromBotCommands.version,
                    latestBotCommands.groupId, latestBotCommands.artifactId, latestBotCommands.version
                )
        }

        override fun formatBCJitpack(
            buildToolType: BuildToolType,
            latestBotCommands: ArtifactInfo
        ) = throw UnsupportedDependencyException("Jitpack dependencies should not have full scripts")

        override fun formatJDA(buildToolType: BuildToolType, version: ArtifactInfo): String =
            readResource("build_scripts/${buildToolType.folderName}/JDA.${buildToolType.fileExtension}")
                .format(version.groupId, version.artifactId, version.version)

        override fun formatJitpack(buildToolType: BuildToolType, version: ArtifactInfo) =
            throw UnsupportedDependencyException("Jitpack dependencies should not have full scripts")
    }

    object Dependencies : BuildScriptSupplier {
        override fun formatBC(
            buildToolType: BuildToolType,
            jdaVersionFromBotCommands: ArtifactInfo,
            latestBotCommands: ArtifactInfo
        ): String = readResource("dependencies_scripts/${buildToolType.folderName}/BotCommands.txt")
            .format(
                jdaVersionFromBotCommands.groupId, jdaVersionFromBotCommands.artifactId, jdaVersionFromBotCommands.version,
                latestBotCommands.groupId, latestBotCommands.artifactId, latestBotCommands.version
            )

        override fun formatBCJitpack(
            buildToolType: BuildToolType,
            latestBotCommands: ArtifactInfo
        ): String = readResource("dependencies_scripts/${buildToolType.folderName}/BotCommands_Jitpack.txt")
            .format(
                latestBotCommands.groupId, latestBotCommands.artifactId, latestBotCommands.version
            )

        override fun formatJDA(buildToolType: BuildToolType, version: ArtifactInfo): String =
            readResource("dependencies_scripts/${buildToolType.folderName}/JDA.txt")
                .format(version.groupId, version.artifactId, version.version)

        override fun formatJitpack(buildToolType: BuildToolType, version: ArtifactInfo): String =
            readResource("dependencies_scripts/${buildToolType.folderName}/Jitpack.txt")
                .format(version.groupId, version.artifactId, version.version)
    }

    companion object {
        fun of(scriptType: ScriptType): BuildScriptSupplier = when (scriptType) {
            ScriptType.DEPENDENCIES -> Dependencies
            ScriptType.FULL -> Full
        }
    }
}

@Throws(UnsupportedDependencyException::class)
private fun readResource(path: String): String {
    val stream = BuildScriptSupplier::class.java.getResourceAsStream(path)
        ?: throw UnsupportedDependencyException("Unable to find the DependencySupplier resource: $path")
    return stream.readAllBytes().decodeToString()
}
