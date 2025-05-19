package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.declarations.MethodDocParameter
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertEquals

object MethodDocParameterTest {

    @Test
    fun `Requires the 'parameters' class on the parsed node`() {
        assertDoesNotThrow {
            MethodDocParameter.parseParameters(htmlFragment(
                """<span class="parameters">(java.lang.String content, java.lang.String content2)</span>"""
            ))
        }
    }

    @Test
    fun `Non-linked parameters have full types`() {
        val parameters = MethodDocParameter.parseParameters(htmlFragment(
            """
                <span class="parameters">(java.lang.String content)</span>
            """.trimIndent()
        ))

        assertEquals("java.lang.String", parameters[0].type)
        assertEquals("String", parameters[0].simpleType)
    }

    @Test
    fun `Linked parameters have full types`() {
        val parameters = MethodDocParameter.parseParameters(htmlFragment(
            """
                <span class="parameters">(<a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/String.html" title="class or interface in java.lang">String</a> content)</span>
            """.trimIndent()
        ))

        assertEquals("java.lang.String", parameters[0].type)
        assertEquals("String", parameters[0].simpleType)
    }

    @Test
    fun `Read multiple types`() {
        val parameters = MethodDocParameter.parseParameters(htmlFragment(
            """
                <span class="parameters">(<a href="https://docs.oracle.com/javase/8/docs/api/javax/annotation/Nonnull.html"
                                             title="class or interface in javax.annotation" class="external-link">@Nonnull</a>
                <a href="https://docs.oracle.com/javase/8/docs/api/java/lang/String.html" title="class or interface in java.lang"
                   class="external-link">String</a>&nbsp;name,
                boolean&nbsp;ignoreCase)</span>
            """.trimIndent()
        ))

        assertEquals("java.lang.String", parameters[0].type)
        assertEquals("String", parameters[0].simpleType)
    }

    @Test
    fun `Nested classes have full types`() {
        val parameters = MethodDocParameter.parseParameters(htmlFragment(
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
        ))

        assertEquals("net.dv8tion.jda.api.entities.Activity.ActivityType", parameters[0].type)
        assertEquals("Activity.ActivityType", parameters[0].simpleType)

        assertEquals("java.lang.String", parameters[1].type)
        assertEquals("String", parameters[1].simpleType)

        assertEquals("java.lang.String", parameters[2].type)
        assertEquals("String", parameters[2].simpleType)
    }

    @Test
    fun `Type parameters are used as-is`() {
        val parameters = MethodDocParameter.parseParameters(htmlFragment(
            """
                <span class="parameters">(<a href="https://docs.oracle.com/javase/8/docs/api/javax/annotation/Nonnull.html"
                                             title="class or interface in javax.annotation" class="external-link">@Nonnull</a>
                <a href="OrderAction.html" title="type parameter in OrderAction">T</a>&nbsp;other)</span>
            """.trimIndent()
        ))

        assertEquals("T", parameters[0].type)
        assertEquals("T", parameters[0].simpleType)
    }
}