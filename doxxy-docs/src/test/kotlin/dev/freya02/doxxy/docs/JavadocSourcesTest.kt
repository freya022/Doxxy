package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.JavadocSource.PackageMatcher.Companion.recursive
import dev.freya02.doxxy.docs.JavadocSource.PackageMatcher.Companion.single
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JavadocSourcesTest {

    @Test
    fun `Test single package matcher`() {
        val source = JavadocSource(
            name = "JDA",
            sourceUrl = "sourceUrl",
            onlineURL = null,
            packageMatchers = listOf(
                single("net.dv8tion.jda"),
            )
        )

        assertTrue(source.isValidPackage("net.dv8tion.jda"))
        assertFalse(source.isValidPackage("net.dv8tion.jda.api"))
        assertFalse(source.isValidPackage("java.lang"))
    }

    @Test
    fun `Test recursive package matcher`() {
        val source = JavadocSource(
            name = "JDA",
            sourceUrl = "sourceUrl",
            onlineURL = null,
            packageMatchers = listOf(
                recursive("net.dv8tion.jda"),
            )
        )

        assertTrue(source.isValidPackage("net.dv8tion.jda"))
        assertTrue(source.isValidPackage("net.dv8tion.jda.api"))
        assertTrue(source.isValidPackage("net.dv8tion.jda.internal"))
        assertFalse(source.isValidPackage("net.dv8tion"))
        assertFalse(source.isValidPackage("java.lang"))
    }
}