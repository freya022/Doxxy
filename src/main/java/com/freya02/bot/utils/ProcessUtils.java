package com.freya02.bot.utils;

import com.freya02.botcommands.api.Logging;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProcessUtils {
	private static final Logger LOGGER = Logging.getLogger();
	private static final ExecutorService OUTPUT_BLACKHOLE_SERVICE = Executors.newCachedThreadPool(r -> {
		final Thread thread = new Thread(r);
		thread.setDaemon(true);

		return thread;
	});

	public static Process runAndWait(String cmdLine, Path workingDir) throws IOException, InterruptedException {
		final Process process = Runtime.getRuntime().exec(cmdLine, null, workingDir.toFile());

		OUTPUT_BLACKHOLE_SERVICE.submit(() -> {
			final byte[] b = new byte[8192];

			try {
				final InputStream inputStream = process.getInputStream();

				while (process.isAlive()) {
					inputStream.read(b);
				}
			} catch (IOException e) {
				LOGGER.error("An error occurred while reading the output stream of {}", cmdLine, e);
			}
		});

		final StringBuilder errorStreamBuilder = new StringBuilder();
		OUTPUT_BLACKHOLE_SERVICE.submit(() -> {
			final char[] chars = new char[8192];

			try {
				final BufferedReader errorReader = process.errorReader();

				while (process.isAlive()) {
					final int read = errorReader.read(chars);

					if (read > 0) {
						errorStreamBuilder.append(chars, 0, read);
					}
				}
			} catch (IOException e) {
				LOGGER.error("An error occurred while reading the error stream of {}", cmdLine, e);
			}
		});

		final int exitCode = process.waitFor();

		if (exitCode != 0) {
			throw new IOException(cmdLine + " has exited with code " + exitCode + ". Error stream:\n" + errorStreamBuilder.toString().stripTrailing());
		}

		return process;
	}
}
