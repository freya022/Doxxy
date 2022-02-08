package com.freya02.bot.utils;

import java.io.IOException;
import java.nio.file.Path;

public class ProcessUtils {
	public static Process runAndWait(String cmdLine, Path workingDir) throws IOException, InterruptedException {
		final Process process = Runtime.getRuntime().exec(cmdLine, null, workingDir.toFile());
		final int exitCode = process.waitFor();

		if (exitCode != 0) {
			throw new IOException(cmdLine + " has exited with code " + exitCode);
		}

		return process;
	}
}
