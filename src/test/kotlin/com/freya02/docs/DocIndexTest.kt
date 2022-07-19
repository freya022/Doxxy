package com.freya02.docs

import com.freya02.bot.Config.Companion.getConfig
import com.freya02.bot.db.Database
import com.freya02.bot.docs.index.DocIndex
import com.freya02.bot.docs.index.ReindexData
import com.freya02.docs.DocWebServer.startDocWebServer
import kotlin.system.exitProcess

suspend fun main() {
    startDocWebServer()

    val config = getConfig()
    val database = Database(config)

    val bcIndex = DocIndex(DocSourceType.BOT_COMMANDS, database)
    val jdaIndex = DocIndex(DocSourceType.JDA, database)
    val javaIndex = DocIndex(DocSourceType.JAVA, database)

//    bcIndex.reindex()
    jdaIndex.reindex(ReindexData("https://github.com/DV8FromTheWorld/JDA/tree/master/src/main/java/"))
//    javaIndex.reindex()

    for (index in listOf(bcIndex, jdaIndex, javaIndex)) {
        val cachedClass = index.getClassDoc("AppOption")
        val cachedMethod = index.getMethodDoc("AppOption#autocomplete()")
        val cachedField = index.getFieldDoc("AppendMode#SET")
        val methodSignatures = index.findMethodSignatures("AppOption")
        val allMethodSignatures = index.findAnyMethodSignatures()
        val fieldSignatures = index.findFieldSignatures("AppendMode")
        val allFieldSignatures = index.findAnyFieldSignatures()
        val methodAndFieldSignatures = index.findMethodAndFieldSignatures("ApplicationCommandInfoMapView")
        val simpleNameList = index.getClasses()
        val classesWithMethods = index.getClassesWithMethods()
        val classesWithFields = index.getClassesWithFields()

        println()
    }

    exitProcess(0)
}