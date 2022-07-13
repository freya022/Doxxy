package com.freya02.bot

import com.freya02.bot.Config.Companion.getConfig
import com.freya02.bot.db.Database
import com.freya02.bot.docs.index.DocIndexKt
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

        val docIndexKt = DocIndexKt(DocSourceType.BOT_COMMANDS, database)

//        docIndexKt.reindex()

        val cachedClass = docIndexKt.getClassDoc("AppOption")
        val cachedMethod = docIndexKt.getMethodDoc("AppOption#autocomplete()")
        val cachedField = docIndexKt.getFieldDoc("AppendMode#SET")
        val methodSignatures = docIndexKt.findMethodSignatures("AppOption")
        val allMethodSignatures = docIndexKt.getAllMethodSignatures()
        val fieldSignatures = docIndexKt.findFieldSignatures("AppendMode")
        val allFieldSignatures = docIndexKt.getAllFieldSignatures()
        val methodAndFieldSignatures = docIndexKt.findMethodAndFieldSignatures("ApplicationCommandInfoMapView")
        val simpleNameList = docIndexKt.getClasses()
        val classesWithMethods = docIndexKt.getClassesWithMethods()
        val classesWithFields = docIndexKt.getClassesWithFields()

        println()

        exitProcess(0)
    }
}