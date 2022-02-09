package com.freya02.bot.versioning.github;

import com.freya02.bot.utils.HttpUtils;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GithubUtils {
	@NotNull
	public static GithubBranch getLatestBranch(String ownerName, String artifactId) throws IOException {
		final List<GithubBranch> branches = getBranches(ownerName, artifactId);

		return branches.stream()
				.filter(s -> s.branchName().matches("\\d\\.\\d\\.\\d"))
				.max(Comparator.comparing(GithubBranch::branchName))
				.orElseGet(() -> branches.get(0));
	}

	@NotNull
	public static String getLatestHash(String ownerName, String repoName, String branchName) throws IOException {
		final HttpUrl url = HttpUrl.get("https://api.github.com/repos/%s/%s/commits".formatted(ownerName, repoName))
				.newBuilder()
				.addQueryParameter("page", "1")
				.addQueryParameter("per_page", "1")
				.addQueryParameter("sha", branchName)
				.build();

		try (Response response = HttpUtils.CLIENT.newCall(new Request.Builder()
						.url(url)
						.header("Accept", "applications/vnd.github.v3+json")
						.build())
				.execute()) {

			final String json = response.body().string();

			return DataArray.fromJson(json).getObject(0).getString("sha").substring(0, 10);
		}
	}

	@NotNull
	public static List<GithubBranch> getBranches(String ownerName, String repoName) throws IOException {
		final HttpUrl url = HttpUrl.get("https://api.github.com/repos/%s/%s/branches".formatted(ownerName, repoName))
				.newBuilder()
				.addQueryParameter("page", "1")
				.addQueryParameter("per_page", "30")
				.build();

		try (Response response = HttpUtils.CLIENT.newCall(new Request.Builder()
						.url(url)
						.header("Accept", "applications/vnd.github.v3+json")
						.build())
				.execute()) {

			final String json = response.body().string();

			final List<GithubBranch> branchList = new ArrayList<>();

			final DataArray branches = DataArray.fromJson(json);
			for (int i = 0; i < branches.length(); i++) {
				final DataObject branchObject = branches.getObject(i);

				final String name = branchObject.getString("name");
				final String sha = branchObject.getObject("commit").getString("sha");

				branchList.add(new GithubBranch(name, sha));
			}

			return branchList;
		}
	}
}
