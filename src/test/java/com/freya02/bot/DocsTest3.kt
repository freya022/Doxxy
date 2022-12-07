package com.freya02.bot

import com.freya02.bot.utils.HttpUtils.getDocument
import com.freya02.docs.ClassDocs.Companion.getUpdatedSource
import com.freya02.docs.DocSourceType
import com.freya02.docs.DocWebServer.startDocWebServer
import com.freya02.docs.DocsSession
import com.freya02.docs.data.ClassDoc
import com.freya02.docs.data.DocDetailType
import java.util.*

object DocsTest3 {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
//        val session = DocsSession()
//        val deprecationClassTest =
//            session.retrieveDoc("https://docs.oracle.com/en/java/javase/17/docs/api/jdk.jartool/com/sun/jarsigner/ContentSigner.html")
//        val deprecationMethodTest =
//            session.retrieveDoc("https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/System.html#getSecurityManager()")
//        val deprecationFieldTest =
//            session.retrieveDoc("https://docs.oracle.com/en/java/javase/17/docs/api/jdk.accessibility/com/sun/java/accessibility/util/AWTEventMonitor.html#containerListener")
//        val arraysTest =
//            session.retrieveDoc("https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Arrays.html")
//        val enumTest =
//            session.retrieveDoc("https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/StandardCopyOption.html")

        startDocWebServer()
        val updatedSource = getUpdatedSource(DocSourceType.JDA)
        val document =
            getDocument("https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/StandardCopyOption.html")

        val url = updatedSource.getSimpleNameToUrlMap()["OptionData"]
        val onlineClassDoc = ClassDoc(
            DocsSession(),
            "https://ci.dv8tion.net/job/JDA5/javadoc/net/dv8tion/jda/api/entities/MessageType.html"
        )
        val classDoc = ClassDoc(DocsSession(), url!!)
        val methodDoc = classDoc.getMethodDocs()["putRolePermissionOverride(long,long,long)"]
        val simpleAnnotatedSignature = methodDoc!!.getSimpleAnnotatedSignature(classDoc)
        val markdown = methodDoc.descriptionElements.toMarkdown("\n")
        val markdown2 =
            methodDoc.getDetails(EnumSet.of(DocDetailType.SPECIFIED_BY)).stream().findFirst().get().toMarkdown("\n")

//        println(
//            "onlineClassDoc.getDescriptionElements().getMarkdown() = " + onlineClassDoc.descriptionElements.toMarkdown(
//                "\n"
//            )
//        )
//
//        println("classDoc      .getDescriptionElements().getMarkdown() = " + classDoc.descriptionElements.toMarkdown("\n"))
//        println()
    }
}