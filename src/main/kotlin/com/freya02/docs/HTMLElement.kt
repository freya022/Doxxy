package com.freya02.docs

import com.freya02.bot.utils.HttpUtils.doesStartByLocalhost
import com.freya02.bot.utils.JDocUtil
import org.jetbrains.annotations.Contract
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.select.NodeVisitor

class HTMLElement private constructor(val targetElement: Element) {
    val asMarkdown: String by lazy {
        targetElement.traverse(object : NodeVisitor {
            override fun head(node: Node, depth: Int) {
                if (node is Element) {
                    if (!node.tagName().equals("a", ignoreCase = true)) {
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
            }

            override fun tail(node: Node, depth: Int) {}
        })

        val html = targetElement.outerHtml()
//				.replaceAll("<code>([^/]*?)<a href=\"(.*?)\">(\\X*?)</a>([^/]*?)</code>", "<code>$1</code><a href=\"$2\"><code>$1</code></a><code>right</code>");

         JDocUtil.formatText(html, targetElement.baseUri())
    }

    override fun toString(): String {
        return targetElement.wholeText()
    }

    companion object {
        @Contract("null -> fail; !null -> new", pure = true)
        fun wrap(targetElement: Element?): HTMLElement = when (targetElement) {
           null -> throw DocParseException()
           else -> HTMLElement(targetElement)
       }

        @Contract(value = "null -> null; !null -> new", pure = true)
        fun tryWrap(targetElement: Element?): HTMLElement? = when {
            targetElement != null -> HTMLElement(targetElement)
            else -> null
        }
    }
}