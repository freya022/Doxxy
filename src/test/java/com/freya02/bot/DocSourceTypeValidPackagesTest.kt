package com.freya02.bot;

import com.freya02.docs.DocSourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DocSourceTypeValidPackagesTest {
	@Test
	public void testJDA() {
		assertTrue(DocSourceType.JDA.isValidPackage("net.dv8tion.jda"));
		assertFalse(DocSourceType.JDA.isValidPackage("java.lang"));
	}

	@Test
	public void testBotCommands() {
		assertTrue(DocSourceType.BOT_COMMANDS.isValidPackage("com.freya02.botcommands.api"));
		assertFalse(DocSourceType.BOT_COMMANDS.isValidPackage("java.lang"));
	}

	@Test
	public void testJava() {
		assertTrue(DocSourceType.JAVA.isValidPackage("java.lang"));
		assertFalse(DocSourceType.JAVA.isValidPackage("com.freya02.botcommands.api"));

		assertTrue(DocSourceType.JAVA.isValidPackage("java.io"));
		assertTrue(DocSourceType.JAVA.isValidPackage("java.lang"));
		assertTrue(DocSourceType.JAVA.isValidPackage("java.lang.annotation"));
		assertTrue(DocSourceType.JAVA.isValidPackage("java.lang.invoke"));
		assertTrue(DocSourceType.JAVA.isValidPackage("java.lang.reflect"));
		assertTrue(DocSourceType.JAVA.isValidPackage("java.math"));
		assertTrue(DocSourceType.JAVA.isValidPackage("java.nio"));
		assertTrue(DocSourceType.JAVA.isValidPackage("java.nio.file"));
		assertTrue(DocSourceType.JAVA.isValidPackage("java.sql"));
		assertTrue(DocSourceType.JAVA.isValidPackage("java.time"));
		assertTrue(DocSourceType.JAVA.isValidPackage("java.util"));
		assertTrue(DocSourceType.JAVA.isValidPackage("java.util.concurrent"));
		assertTrue(DocSourceType.JAVA.isValidPackage("java.util.concurrent.atomic"));
		assertTrue(DocSourceType.JAVA.isValidPackage("java.util.concurrent.locks"));
		assertTrue(DocSourceType.JAVA.isValidPackage("java.util.function"));
		assertTrue(DocSourceType.JAVA.isValidPackage("java.util.random"));
		assertTrue(DocSourceType.JAVA.isValidPackage("java.util.regex"));
		assertTrue(DocSourceType.JAVA.isValidPackage("java.util.stream"));

		assertFalse(DocSourceType.JAVA.isValidPackage("java.lang.constant"));
		assertFalse(DocSourceType.JAVA.isValidPackage("java.lang.instrument"));
		assertFalse(DocSourceType.JAVA.isValidPackage("java.lang.management"));
		assertFalse(DocSourceType.JAVA.isValidPackage("java.lang.module"));
		assertFalse(DocSourceType.JAVA.isValidPackage("java.lang.ref"));
		assertFalse(DocSourceType.JAVA.isValidPackage("java.lang.runtime"));
		assertFalse(DocSourceType.JAVA.isValidPackage("java.net"));
		assertFalse(DocSourceType.JAVA.isValidPackage("java.nio.channels.spi"));
		assertFalse(DocSourceType.JAVA.isValidPackage("java.nio.charsets"));
		assertFalse(DocSourceType.JAVA.isValidPackage("java.nio.charsets.spi"));
		assertFalse(DocSourceType.JAVA.isValidPackage("java.nio.file.attribute"));
		assertFalse(DocSourceType.JAVA.isValidPackage("java.nio.file.spi"));
		assertFalse(DocSourceType.JAVA.isValidPackage("java.rmi"));
		assertFalse(DocSourceType.JAVA.isValidPackage("java.text"));
		assertFalse(DocSourceType.JAVA.isValidPackage("java.util.jar"));
		assertFalse(DocSourceType.JAVA.isValidPackage("java.util.logging"));
		assertFalse(DocSourceType.JAVA.isValidPackage("java.util.spi"));
		assertFalse(DocSourceType.JAVA.isValidPackage("java.util.zip"));

		assertFalse(DocSourceType.JAVA.isValidPackage("javaxutil.stream"));
	}
}
