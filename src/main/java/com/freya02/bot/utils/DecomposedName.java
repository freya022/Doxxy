package com.freya02.bot.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
}
