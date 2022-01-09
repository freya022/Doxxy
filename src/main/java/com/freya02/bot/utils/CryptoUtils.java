package com.freya02.bot.utils;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CryptoUtils {
	private static final MessageDigest SHA3_256;

	static {
		try {
			SHA3_256 = MessageDigest.getInstance("SHA3-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	@NotNull
	public static String hash(byte[] content) {
		return toHexString(SHA3_256.digest(content));
	}

	public static String toHexString(final byte[] bytes) {
		final StringBuilder builder = new StringBuilder(bytes.length * 2);

		for (final byte b : bytes) {
			final int var = 0xFF & b;

			if (var < 0x10) {
				builder.append("0");
			}

			builder.append(Integer.toHexString(var).toUpperCase());
		}

		return builder.toString();
	}

	@NotNull
	public static String hash(@NotNull String content) {
		return hash(content.getBytes(StandardCharsets.UTF_8));
	}
}
