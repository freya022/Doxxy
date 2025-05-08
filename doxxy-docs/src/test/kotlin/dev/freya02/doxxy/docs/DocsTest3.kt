package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.ClassDocs.Companion.getUpdatedSource
import dev.freya02.doxxy.docs.declarations.JavadocClass
import dev.freya02.doxxy.docs.sections.DocDetail
import dev.freya02.doxxy.docs.utils.HttpUtils.getDocument
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

//        startDocWebServer()
        val updatedSource = getUpdatedSource(DocSourceType.JDA)
        val document =
            getDocument("https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/StandardCopyOption.html")

        val url = updatedSource.simpleNameToUrlMap["OptionData"]
        val onlineClass = JavadocClass(
            DocsSession(),
            "https://docs.jda.wiki/net/dv8tion/jda/api/entities/MessageType.html"
        )
        val clazz = JavadocClass(DocsSession(), url!!)
        val method = clazz.methods["putRolePermissionOverride(long,long,long)"]
        val simpleAnnotatedSignature = method!!.getSimpleAnnotatedSignature(clazz)
        val markdown = method.descriptionElements.toMarkdown("\n")
        val markdown2 =
            method.getDetails(EnumSet.of(DocDetail.Type.SPECIFIED_BY)).stream().findFirst().get().toMarkdown("\n")

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