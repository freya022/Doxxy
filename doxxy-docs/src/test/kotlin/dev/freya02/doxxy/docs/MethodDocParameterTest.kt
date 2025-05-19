package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.declarations.MethodDocParameters
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertEquals

object MethodDocParameterTest {

    private val moduleSession = mockk<JavadocModuleSession> {
        every { source } returns JDK_SOURCE
    }

    @Test
    fun `Requires the 'parameters' class on the parsed node`() {
        assertDoesNotThrow {
            MethodDocParameters(htmlFragment(
                """<span class="parameters">(java.lang.String content, java.lang.String content2)</span>"""
            ))
        }
    }

    @Test
    fun `Non-linked parameters have full types`() {
        val params = MethodDocParameters(htmlFragment(
            """
                <span class="parameters">(java.lang.String content)</span>
            """.trimIndent()
        ))
        val param = params.parameters[0]

        assertEquals("java.lang.String", param.type)
        assertEquals("String", param.simpleType)
    }

    @Test
    fun `Linked parameters have full types`() {
        val params = MethodDocParameters(htmlFragment(
            """
                <span class="parameters">(<a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/String.html" title="class or interface in java.lang">String</a> content)</span>
            """.trimIndent()
        ))
        val param = params.parameters[0]

        assertEquals("java.lang.String", param.type)
        assertEquals("String", param.simpleType)
    }

    @Test
    fun `Read multiple types`() {
        val params = MethodDocParameters(htmlFragment(
            """
                <span class="parameters">(<a href="https://docs.oracle.com/javase/8/docs/api/javax/annotation/Nonnull.html"
                                             title="class or interface in javax.annotation" class="external-link">@Nonnull</a>
                <a href="https://docs.oracle.com/javase/8/docs/api/java/lang/String.html" title="class or interface in java.lang"
                   class="external-link">String</a>&nbsp;name,
                boolean&nbsp;ignoreCase)</span>
            """.trimIndent()
        ))
        val param = params.parameters[0]

        assertEquals("java.lang.String", param.type)
        assertEquals("String", param.simpleType)
    }

    @Test
    fun `Nested classes have full types`() {
        val params = MethodDocParameters(htmlFragment(
            """
                <span class="parameters">(<a href="https://docs.oracle.com/javase/8/docs/api/javax/annotation/Nonnull.html"
                                             title="class or interface in javax.annotation" class="external-link">@Nonnull</a>
                <a href="Activity.ActivityType.html" title="enum in net.dv8tion.jda.api.entities">Activity.ActivityType</a>&nbsp;type,
                <a href="https://docs.oracle.com/javase/8/docs/api/javax/annotation/Nonnull.html"
                   title="class or interface in javax.annotation" class="external-link">@Nonnull</a>
                <a href="https://docs.oracle.com/javase/8/docs/api/java/lang/String.html" title="class or interface in java.lang"
                   class="external-link">String</a>&nbsp;name,
                <a href="https://docs.oracle.com/javase/8/docs/api/javax/annotation/Nullable.html"
                   title="class or interface in javax.annotation" class="external-link">@Nullable</a>
                <a href="https://docs.oracle.com/javase/8/docs/api/java/lang/String.html" title="class or interface in java.lang"
                   class="external-link">String</a>&nbsp;url)</span>
            """.trimIndent()
        )).parameters

        assertEquals("net.dv8tion.jda.api.entities.Activity.ActivityType", params[0].type)
        assertEquals("Activity.ActivityType", params[0].simpleType)

        assertEquals("java.lang.String", params[1].type)
        assertEquals("String", params[1].simpleType)

        assertEquals("java.lang.String", params[2].type)
        assertEquals("String", params[2].simpleType)
    }
}