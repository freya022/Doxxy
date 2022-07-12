package com.freya02.bot.utils;

import com.freya02.docs.DocSourceType;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record DecomposedName(@Nullable String packageName, @NotNull String className) {
	@NotNull
	public static String getSimpleClassName(@NotNull String fullName) {
		for (int i = 0; i < fullName.length() - 1; i++) {
			if (fullName.charAt(i) == '.' && Character.isUpperCase(fullName.charAt(i + 1))) {
				return fullName.substring(i + 1);
			}
		}

		//No package if naming conventions are respected
		return fullName;
	}

	public static @Nullable String getPackageName(@NotNull String fullName) {
		for (int i = 0; i < fullName.length() - 1; i++) {
			if (fullName.charAt(i) == '.' && Character.isUpperCase(fullName.charAt(i + 1))) {
				return fullName.substring(0, i);
			}
		}

		//No package if naming conventions are respected
		return null;
	}

	@Contract("_ -> new")
	@NotNull
	public static DecomposedName getDecomposition(@NotNull String fullName) {
		for (int i = 0; i < fullName.length() - 1; i++) {
			if (fullName.charAt(i) == '.' && Character.isUpperCase(fullName.charAt(i + 1))) {
				return new DecomposedName(fullName.substring(0, i), fullName.substring(i + 1));
			}
		}

		//No package if naming conventions are respected
		return new DecomposedName(null, fullName);
	}

	@Contract("_, _ -> new")
	@NotNull
	public static DecomposedName getDecompositionFromUrl(@NotNull DocSourceType sourceType, @NotNull String target) {
		final HttpUrl sourceUrl = HttpUrl.get(sourceType.getSourceUrl());
		final HttpUrl targetUrl = HttpUrl.get(target);

		final List<String> rightSegments = new ArrayList<>(targetUrl.pathSegments().subList(sourceUrl.pathSize(), targetUrl.pathSize()));

		//Remove java 9 modules from segments
		if (rightSegments.get(0).startsWith("java.")) rightSegments.remove(0);

		//All segments except last
		final List<String> packageSegments = rightSegments.subList(0, rightSegments.size() - 1);

		final String lastSegment = rightSegments.get(rightSegments.size() - 1);

		//Remove .html extension
		final String className = lastSegment.substring(0, lastSegment.lastIndexOf('.'));

		return new DecomposedName(String.join(".", packageSegments), className);
	}
}
