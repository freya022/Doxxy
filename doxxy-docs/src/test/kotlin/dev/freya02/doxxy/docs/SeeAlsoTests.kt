package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.sections.DocDetail
import dev.freya02.doxxy.docs.sections.SeeAlso
import io.mockk.every
import io.mockk.mockk
import org.jsoup.Jsoup
import kotlin.test.Test
import kotlin.test.assertEquals

class SeeAlsoTests {

    @Test
    fun `References across modules have correct links`() {
        val detail = DocDetail(DocDetail.Type.SEE_ALSO, listOf(
            JavadocElement.wrap(Jsoup.parse("""
                <dd>
                <ul class="tag-list-long">
                <li><a href="https://docs.oracle.com/javase/8/docs/api/java/lang/Enum.html">Enum</a></li>
                </ul>
                </dd>
            """.trimIndent(), "${DocSourceType.JDA.sourceUrl}/net/dv8tion/jda/api/entities/Activity.ActivityType.html"))
        ))

        val moduleSession = mockk<JavadocModuleSession> {
            every { source } returns JDA_SOURCE
            every { globalSession } returns mockk<GlobalJavadocSession> {
                every { sources } returns JavadocSources(listOf(JDA_SOURCE, JDK_SOURCE))
                every { retrieveSession(JDK_SOURCE) } returns mockk<JavadocModuleSession> {
                    every { isValidURL("https://docs.oracle.com/javase/8/docs/api/java/lang/Enum.html") } returns true
                }
            }
        }
        val seeAlso = SeeAlso(moduleSession, detail)

        val references = seeAlso.references
        assertEquals(1, references.size)
        assertEquals("https://docs.oracle.com/javase/8/docs/api/java/lang/Enum.html", references.first().link)
    }
}