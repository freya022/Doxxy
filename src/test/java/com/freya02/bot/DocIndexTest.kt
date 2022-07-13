package com.freya02.bot

import com.freya02.bot.Config.Companion.getConfig
import com.freya02.bot.db.Database
import com.freya02.bot.docs.index.DocIndex
import com.freya02.docs.DocSourceType
import com.freya02.docs.DocWebServer.startDocWebServer
import kotlin.system.exitProcess

object DocIndexTest {
    @JvmStatic
    fun main(args: Array<String>) {
        startDocWebServer()

//        val bcIndex = DocIndex(DocSourceType.BOT_COMMANDS).reindex()
//        val jdaIndex = DocIndex(DocSourceType.JDA).reindex()
//        val javaIndex = DocIndex(DocSourceType.JAVA).reindex()
//
//        println()
//
//        bcIndex.close()
//        jdaIndex.close()
//        javaIndex.close()

        val config = getConfig()
        val database = Database(config)

        val bcIndex = DocIndex(DocSourceType.BOT_COMMANDS, database)
        val jdaIndex = DocIndex(DocSourceType.JDA, database)
        val javaIndex = DocIndex(DocSourceType.JAVA, database)

//        bcIndex.reindex()
//        jdaIndex.reindex()
//        javaIndex.reindex()

        for (index in listOf(bcIndex, jdaIndex, javaIndex)) {
            val cachedClass = index.getClassDoc("AppOption")
            val cachedMethod = index.getMethodDoc("AppOption#autocomplete()")
            val cachedField = index.getFieldDoc("AppendMode#SET")
            val methodSignatures = index.findMethodSignatures("AppOption")
            val allMethodSignatures = index.getAllMethodSignatures()
            val fieldSignatures = index.findFieldSignatures("AppendMode")
            val allFieldSignatures = index.getAllFieldSignatures()
            val methodAndFieldSignatures = index.findMethodAndFieldSignatures("ApplicationCommandInfoMapView")
            val simpleNameList = index.getClasses()
            val classesWithMethods = index.getClassesWithMethods()
            val classesWithFields = index.getClassesWithFields()

            println()
        }

        exitProcess(0)
    }
}