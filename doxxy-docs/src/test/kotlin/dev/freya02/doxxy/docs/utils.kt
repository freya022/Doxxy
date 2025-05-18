package dev.freya02.doxxy.docs

import org.intellij.lang.annotations.Language
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.nio.file.Path
import kotlin.io.path.toPath

fun getResourcePath(resource: String): Path {
    return IntegrationTests::class.java.getResource(resource)!!.toURI().toPath()
}

fun htmlFragment(@Language("html") html: String): Element {
    return Jsoup.parseBodyFragment(html).selectFirst("html > body > *")!!
}