package com.freya02.bot.utils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

public class Utils {
	@NotNull
	public static String readResource(@NotNull String url) {
		final Class<?> callerClass = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
		try (InputStream stream = callerClass.getResourceAsStream(url)) {
			if (stream == null) throw new NoSuchElementException("Resource of class " + callerClass.getSimpleName() + " at URL '" + url + "' does not exist");

			return new String(stream.readAllBytes());
		} catch (IOException e) {
			throw new RuntimeException("Unable to read resource of class " + callerClass.getSimpleName() + " at URL '" + url + "'", e);
		}
	}

	@NotNull
	public static String getClassName(String fullName) {
		for (int i = 0, length = fullName.length(); i < length; i++) {
			final char c = fullName.charAt(i);

			if (Character.isUpperCase(c)) {
				return fullName.substring(i);
			}
		}

		throw new IllegalArgumentException("Could not get glass name from '" + fullName + "'");
	}
}
