package com.freya02.bot.versioning.maven;

import com.freya02.bot.utils.HttpUtils;
import com.freya02.bot.versioning.ArtifactInfo;
import net.dv8tion.jda.api.exceptions.ParsingException;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class MavenUtils {
	public static final String MAVEN_METADATA_FORMAT = "https://repo.maven.apache.org/maven2/%s/%s/maven-metadata.xml";
	public static final String MAVEN_JAVADOC_FORMAT = "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s-javadoc.jar";
	public static final String M2_METADATA_FORMAT = "https://m2.dv8tion.net/releases/%s/%s/maven-metadata.xml";

	@NotNull
	public static String getLatestMavenVersion(String formatUrl, String groupId, String artifactId) throws IOException {
		final Document document = getMavenMetadata(formatUrl, groupId, artifactId);

		final Element latestElement = document.selectFirst("metadata > versioning > latest");
		if (latestElement == null) throw new ParsingException("Unable to parse latest version");

		return latestElement.text();
	}

	@NotNull
	public static Document getMavenMetadata(String formatUrl, String groupId, String artifactId) throws IOException {
		return HttpUtils.getDocument(formatUrl.formatted(groupId.replace('.', '/'), artifactId));
	}

	public static void downloadMavenDocs(@NotNull ArtifactInfo artifactInfo, @NotNull Path targetPath) throws IOException {
		try (Response response = HttpUtils.CLIENT.newCall(new Request.Builder()
						.url(MAVEN_JAVADOC_FORMAT.formatted(artifactInfo.groupId().replace('.', '/'),
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
