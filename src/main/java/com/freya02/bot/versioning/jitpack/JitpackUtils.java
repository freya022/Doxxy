package com.freya02.bot.versioning.jitpack;

import com.freya02.bot.utils.HttpUtils;
import com.freya02.bot.versioning.ArtifactInfo;
import com.google.gson.Gson;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.rmi.RemoteException;
import java.util.Map;

public class JitpackUtils {
	@SuppressWarnings("unchecked")
	private static BuildStatus triggerBuild(String latestCommitHash) throws IOException {
		try (Response response = HttpUtils.CLIENT.newCall(new Request.Builder()
						.url("https://jitpack.io/api/builds/com.github.freya022/BotCommands/%s".formatted(latestCommitHash))
						.build())
				.execute()) {
			final Map<String, ?> map = new Gson().fromJson(response.body().string(), Map.class);

			final String status = (String) map.get("status");

			if (response.code() == 200 && status.equalsIgnoreCase("ok")) {
				return BuildStatus.OK;
			} else if (response.code() == 404 && (status.equalsIgnoreCase("ok") || status.equalsIgnoreCase("none"))) {
				return BuildStatus.IN_PROGRESS;
			} else if (response.code() == 404 && status.equalsIgnoreCase("error")) {
				return BuildStatus.ERROR;
			} else {
				throw new IllegalStateException("Unable to check build status: code = " + response.code() + ", status = '" + status + "'");
			}
		}
	}

	public static BuildStatus waitForBuild(String hash) throws IOException {
		BuildStatus buildStatus = BuildStatus.IN_PROGRESS;
		for (int i = 0; i < 3; i++) {
			while ((buildStatus = triggerBuild(hash)) == BuildStatus.IN_PROGRESS) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					throw new RemoteException("Got interrupted while waiting for a jitpack build to finish", e);
				}
			}

			//Retry up to 3 time if build status isn't OK, sometimes jitpack may shit itself i don't know
			if (buildStatus == BuildStatus.OK) {
				break;
			}
		}

		return buildStatus;
	}

	public static void downloadJitpackDocs(@NotNull ArtifactInfo artifactInfo, @NotNull Path targetPath) throws IOException {
		try (Response response = HttpUtils.CLIENT.newCall(new Request.Builder()
						.url("https://jitpack.io/%s/%s/%s/%s-%s-javadoc.jar".formatted(artifactInfo.groupId().replace('.', '/'),
								artifactInfo.artifactId(),
								artifactInfo.version(),
								artifactInfo.artifactId(),
								artifactInfo.version()))
						.build())
				.execute()) {
			final ResponseBody body = response.body();
			if (body == null) throw new IOException("Got no ResponseBody for " + response.request().url());

			if (!response.isSuccessful()) throw new IOException("Got an unsuccessful response from " + response.request().url() + ", code: " + response.code());

			Files.copy(body.byteStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
