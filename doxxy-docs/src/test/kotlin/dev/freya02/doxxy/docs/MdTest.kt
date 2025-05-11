package dev.freya02.doxxy.docs

import org.jsoup.Jsoup

object MdTest {
    @JvmStatic
    fun main(args: Array<String>) {
//		final String html = "<b><i>lmao</i>not italic</b>not bold<br>newline <code><a href=\"mahlink.html\">Absolutely not a link</a></code>, <code><a href=\"mahlink.html\">Absolutely not a link</a></code>";
        val html =
            "<dd><code><a href=\"https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html?is-external=true\" title=\"class or interface in java.io\" class=\"externalLink\">Serializable</a></code>, <code><a href=\"https://docs.oracle.com/javase/8/docs/api/java/lang/Comparable.html?is-external=true\" title=\"class or interface in java.lang\" class=\"externalLink\">Comparable</a>&lt;<a href=\"Component.Type.html\" title=\"enum in net.dv8tion.jda.api.interactions.components\">Component.Type</a>&gt;</code></dd>"
        testHtml(html)

        println(
            """
    
    ${"-".repeat(50)}
    
    """.trimIndent()
        )

        testHtml("<a href=\"Invite.html#getType()\"><code>Invite.getType()</code></a>")
    }

    private fun testHtml(html: String) {
        val element = JavadocElement.wrap(Jsoup.parseBodyFragment(html, "https://myurl.com").selectFirst("body"))
        println(element.asMarkdown)
    }
}