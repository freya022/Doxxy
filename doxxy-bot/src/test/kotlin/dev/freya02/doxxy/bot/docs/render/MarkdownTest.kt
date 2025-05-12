package dev.freya02.doxxy.bot.docs.render

import dev.freya02.doxxy.docs.JavadocElement
import io.mockk.every
import io.mockk.mockk
import org.jsoup.Jsoup
import kotlin.test.Test
import kotlin.test.assertEquals

object MarkdownTest {

    @Test
    fun `Online links are resolved`() {
        val element = mockk<JavadocElement> {
            every { targetElement } returns Jsoup.parse("""<a href="Invite.html#getType()"><code>Invite.getType()</code></a>""", "https://docs.jda.wiki")
        }
        assertEquals("[`Invite.getType()`](https://docs.jda.wiki/Invite.html#getType%28%29)", element.toMarkdown())
    }

    @Test
    fun `Offline links are removed`() {
        val element = mockk<JavadocElement> {
            every { targetElement } returns Jsoup.parse("""<a href="Invite.html#getType()"><code>Invite.getType()</code></a>""", "http://localhost:9999")
        }
        assertEquals("`Invite.getType()`", element.toMarkdown())
    }
}