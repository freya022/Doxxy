package dev.freya02.doxxy.bot

import dev.freya02.doxxy.bot.versioning.ArtifactInfo
import dev.freya02.doxxy.bot.versioning.supplier.BuildScriptSupplier
import dev.freya02.doxxy.bot.versioning.supplier.BuildToolType
import kotlin.test.Test
import kotlin.test.assertFalse

private val placeholderRegex = Regex("""\{\{\w+}}""")

class BuildScriptSupplierTest {
    @Test
    fun `All placeholders are replaced`() {
        val artifactInfo = ArtifactInfo("dev.freya02", "doxxy", "1.0.0")

        for (buildToolType in BuildToolType.entries) {
            verify(BuildScriptSupplier.Full.formatBC(buildToolType, artifactInfo, artifactInfo))
            verify(BuildScriptSupplier.Full.formatJDA(buildToolType, artifactInfo))

            verify(BuildScriptSupplier.Dependencies.formatBC(buildToolType, artifactInfo, artifactInfo))
            verify(BuildScriptSupplier.Dependencies.formatBCJitpack(buildToolType, artifactInfo))
            verify(BuildScriptSupplier.Dependencies.formatJDA(buildToolType, artifactInfo))
            verify(BuildScriptSupplier.Dependencies.formatJitpack(buildToolType, artifactInfo))
        }
    }

    private fun verify(script: String) = assertFalse(script.contains(placeholderRegex))
}
