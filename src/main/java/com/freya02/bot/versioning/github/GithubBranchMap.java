package com.freya02.bot.versioning.github;

import java.util.Map;

public record GithubBranchMap(GithubBranch defaultBranch, Map<String, GithubBranch> branches) {
}
