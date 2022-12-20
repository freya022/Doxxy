package com.freya02.docs

import com.freya02.bot.Config
import com.freya02.bot.db.Database
import com.freya02.bot.docs.index.DocIndex
import com.freya02.bot.docs.index.ReindexData
import com.freya02.bot.versioning.github.GithubUtils
import com.freya02.docs.DocWebServer.startDocWebServer
import dev.minn.jda.ktx.events.getDefaultScope
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

suspend fun main() {
    getDefaultScope().launch {
        startDocWebServer()

        val config = Config.config
        val database = Database(config)

        val bcIndex = DocIndex(DocSourceType.BOT_COMMANDS, database)
        val jdaIndex = DocIndex(DocSourceType.JDA, database)
        val javaIndex = DocIndex(DocSourceType.JAVA, database)

        bcIndex.reindex(ReindexData())
        val sourceUrl = GithubUtils.getLatestReleaseHash("DV8FromTheWorld", "JDA")
            ?.let { hash -> "https://github.com/DV8FromTheWorld/JDA/blob/${hash.hash}/src/main/java/" }
        jdaIndex.reindex(ReindexData(sourceUrl))
        javaIndex.reindex(ReindexData())

        for (index in listOf(bcIndex, jdaIndex, javaIndex)) {
            val cachedClass = index.getClassDoc("AppOption")
            val cachedMethod = index.getMethodDoc("AppOption#autocomplete()")
            val cachedField = index.getFieldDoc("AppendMode#SET")
            val methodSignatures = index.findMethodSignaturesIn("AppOption")
            val allMethodSignatures = index.findAnyMethodSignatures()
            val fieldSignatures = index.findFieldSignaturesIn("AppendMode")
            val allFieldSignatures = index.findAnyFieldSignatures()
            val methodAndFieldSignatures = index.findMethodAndFieldSignaturesIn("ApplicationCommandInfoMapView")
            val simpleNameList = index.getClasses()
            val classesWithMethods = index.getClassesWithMethods()
            val classesWithFields = index.getClassesWithFields()

            println()
        }

        exitProcess(0)
    }.join()
}