package com.freya02.bot.versioning.jitpack;

import com.freya02.bot.utils.HttpUtils;
import okhttp3.Request;

import java.io.IOException;

public class JitpackUtils {
	public static void triggerBuild(String groupId, String artifactId, String latestCommitHash) throws IOException {
		HttpUtils.CLIENT.newCall(new Request.Builder()
						.url("https://jitpack.io/api/builds/%s/%s/%s".formatted(groupId, artifactId, latestCommitHash))
						.build())
				.execute().close();
	}
}
