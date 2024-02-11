package com.freya02.docs

import com.freya02.bot.utils.JDocUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AutolinkEscapeTest {
    @Test
    fun test() {
        val returnsDetailHtml = """
            <dd>
             <a href="https://docs.jda.wiki/net/dv8tion/jda/api/requests/RestAction.html" title="interface in net.dv8tion.jda.api.requests"><code>RestAction</code></a> - Type: List&lt;<a href="https://docs.jda.wiki/net/dv8tion/jda/api/entities/Invite.html" title="interface in net.dv8tion.jda.api.entities"><code>Invite</code></a>&gt; <br>
             The list of expanded Invite objects
            </dd>
        """.trimIndent()
        val md = JDocUtil.formatText(returnsDetailHtml, "https://docs.jda.wiki/net/dv8tion/jda/api/entities/Guild.html")

        assertEquals("""
            [`RestAction`](https://docs.jda.wiki/net/dv8tion/jda/api/requests/RestAction.html) \- Type: List\<[`Invite`](https://docs.jda.wiki/net/dv8tion/jda/api/entities/Invite.html)\>
            The list of expanded Invite objects
        """.trimIndent(), md)
    }

    @Test
    fun test2() {
        val descriptionHtml = """
            <div class="block">Parses the provided markdown formatting, or unicode characters, to an Emoji instance.
            
             <p><b>Example</b><br>
             </p><pre><code>
             // animated custom emoji
             fromFormatted("&lt;a:dance:123456789123456789&gt;");
             // not animated custom emoji
             fromFormatted("&lt;:dog:123456789123456789&gt;");
             // unicode emoji, escape codes
             fromFormatted("&amp;#92;uD83D&amp;#92;uDE03");
             // codepoint notation
             fromFormatted("U+1F602");
             // unicode emoji
             fromFormatted("😃");
             </code></pre></div>
        """.trimIndent()

        val md = JDocUtil.formatText(descriptionHtml, "https://docs.jda.wiki/net/dv8tion/jda/api/entities/emoji/Emoji.html")

        assertEquals("""
            Parses the provided markdown formatting, or unicode characters, to an Emoji instance.
            
            **Example**
            
            ```java
            // animated custom emoji
             fromFormatted("<a:dance:123456789123456789>");
             // not animated custom emoji
             fromFormatted("<:dog:123456789123456789>");
             // unicode emoji, escape codes
             fromFormatted("\uD83D\uDE03");
             // codepoint notation
             fromFormatted("U+1F602");
             // unicode emoji
             fromFormatted("😃");
            ```
        """.trimIndent(), md)
    }
}