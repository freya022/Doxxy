package com.freya02.bot;

import com.freya02.bot.utils.ProcessUtils;

import java.io.IOException;
import java.nio.file.Path;

public class ProcessTest {
	public static void main(String[] args) throws IOException, InterruptedException {
		ProcessUtils.runAndWait("mvn.cmd -a -xd", Path.of("."));
	}
}
