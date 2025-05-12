package dev.freya02.doxxy.bot.docs.render

import dev.freya02.doxxy.bot.docs.DocSourceType
import dev.freya02.doxxy.bot.docs.render.SubSuperScripts.replaceScriptCharactersOrNull
import dev.freya02.doxxy.bot.utils.HttpUtils.doesStartByLocalhost
import dev.freya02.doxxy.docs.JavadocElement
import dev.freya02.doxxy.docs.JavadocElements
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.select.NodeVisitor

internal fun JavadocElement.toMarkdown(): String {
    targetElement.traverse(object : NodeVisitor {
        override fun head(node: Node, depth: Int) {
            if (node !is Element) return

            if (!node.tagName().equals("a", ignoreCase = true)) {
                if (node.tagName().equals("sub", ignoreCase = true)) {
                    node.text().replaceScriptCharactersOrNull(SubSuperScripts.subscripts)?.let { node.text(it) }
                } else if (node.tagName().equals("sup", ignoreCase = true)) {
                    node.text().replaceScriptCharactersOrNull(SubSuperScripts.superscripts)?.let { node.text(it) }
                }

                return
            }

            val href = node.absUrl("href")

            //Try to resolve into an online link
            for (type in DocSourceType.entries) {
                val effectiveUrl = type.toEffectiveURL(href)
                if (!doesStartByLocalhost(effectiveUrl)) { //If it's a valid link then don't remove it
                    node.attr("href", effectiveUrl)
                    return
                }
            }

            //If no online URL has been found then do not link to localhost href(s)
            node.removeAttr("href")
        }

        override fun tail(node: Node, depth: Int) {}
    })

    val html = targetElement.outerHtml()
//			.replaceAll("<code>([^/]*?)<a href=\"(.*?)\">(\\X*?)</a>([^/]*?)</code>", "<code>$1</code><a href=\"$2\"><code>$1</code></a><code>right</code>");

    return JDocUtil.formatText(html, targetElement.baseUri())
}

fun JavadocElements.toMarkdown(delimiter: String): String = joinToString(delimiter) { it.toMarkdown() }