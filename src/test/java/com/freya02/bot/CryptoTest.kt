package com.freya02.bot;

import com.freya02.bot.utils.CryptoUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class CryptoTest {
	private static final String STR = "setTimeout(long, TimeUnit, Consumer)";
	private static MessageDigest md5, sha1, sha256, sha512, sha3_256;

	public static void main(String[] args) throws Exception {
		md5 = MessageDigest.getInstance("MD5");
		sha1 = MessageDigest.getInstance("SHA-1");
		sha256 = MessageDigest.getInstance("SHA-256");
		sha512 = MessageDigest.getInstance("SHA-512");
		sha3_256 = MessageDigest.getInstance("SHA3-256");

		formatTest();

		TestUtils.measureTime("MD5", 100000, 10000, () -> {
			final byte[] digest = md5.digest(STR.getBytes(StandardCharsets.UTF_8));

			CryptoUtils.toHexString(digest);
		});

		TestUtils.measureTime("SHA1", 100000, 10000, () -> {
			final byte[] digest = sha1.digest(STR.getBytes(StandardCharsets.UTF_8));

			CryptoUtils.toHexString(digest);
		});

		TestUtils.measureTime("SHA256", 100000, 10000, () -> {
			final byte[] digest = sha256.digest(STR.getBytes(StandardCharsets.UTF_8));

			CryptoUtils.toHexString(digest);
		});

		TestUtils.measureTime("SHA512", 100000, 10000, () -> {
			final byte[] digest = sha512.digest(STR.getBytes(StandardCharsets.UTF_8));

			CryptoUtils.toHexString(digest);
		});

		TestUtils.measureTime("SHA3-256", 100000, 10000, () -> {
			final byte[] digest = sha3_256.digest(STR.getBytes(StandardCharsets.UTF_8));

			CryptoUtils.toHexString(digest);
		});
	}

	private static void formatTest() {
		final byte[] digest = sha512.digest(STR.getBytes(StandardCharsets.UTF_8));

		TestUtils.measureTime("String.format", 100000, 100000, () -> {
			String.format("%032X", new BigInteger(1, digest));
		});

		TestUtils.measureTime("String builder", 100000, 100000, () -> {
			CryptoUtils.toHexString(digest);
		});
	}
}
