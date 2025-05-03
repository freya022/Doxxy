package dev.freya02.doxxy.docs

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DocSourceTypeValidPackagesTest {
    @Test
    fun testJDA() {
        Assertions.assertTrue(DocSourceType.JDA.isValidPackage("net.dv8tion.jda"))
        Assertions.assertFalse(DocSourceType.JDA.isValidPackage("java.lang"))
    }

    @Test
    fun testJava() {
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.lang"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("dev.freya02.doxxy.botcommands.api"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.io"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.lang"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.lang.annotation"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.lang.invoke"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.lang.reflect"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.math"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.nio"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.nio.file"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.sql"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.time"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.util"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.util.concurrent"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.util.concurrent.atomic"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.util.concurrent.locks"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.util.function"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.util.random"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.util.regex"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.util.stream"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("java.lang.constant"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("java.lang.instrument"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("java.lang.management"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("java.lang.module"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("java.lang.ref"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("java.lang.runtime"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("java.net"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("java.nio.channels.spi"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("java.nio.charsets"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("java.nio.charsets.spi"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("java.nio.file.attribute"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("java.nio.file.spi"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("java.rmi"))
        Assertions.assertTrue(DocSourceType.JAVA.isValidPackage("java.text"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("java.util.jar"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("java.util.logging"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("java.util.spi"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("java.util.zip"))
        Assertions.assertFalse(DocSourceType.JAVA.isValidPackage("javaxutil.stream"))
    }
}