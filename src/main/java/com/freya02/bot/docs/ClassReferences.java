package com.freya02.bot.docs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static com.freya02.bot.utils.DecomposedName.getSimpleClassName;

public final class ClassReferences {
	//Short name only
	private static final Map<String, ClassReference> map = new HashMap<>();

	@NotNull
	@UnmodifiableView
	public static Map<String, ClassReference> getAllReferences() {
		return Collections.unmodifiableMap(map);
	}

	@Nullable
	public static ClassReference get(@NotNull String name) {
		return map.get(getSimpleClassName(name));
	}

	@NotNull
	public static ClassReference computeIfAbsent(@NotNull String name, @NotNull Function<@NotNull String, @NotNull ClassReference> supplier) {
		name = getSimpleClassName(name);

		final ClassReference reference = map.get(name);

		if (reference == null) {
			final ClassReference newRef = supplier.apply(name);
			map.put(name, newRef);

			return newRef;
		} else {
			final ClassReference updatedRef = supplier.apply(name);
			if (reference.link() == null) { //Try to update link info
				reference.setLink(updatedRef.link());
			}

			if (reference.packageName() == null) { //Try to update package info
				reference.setPackageName(updatedRef.packageName());
			}
		}

		return reference;
	}

	@NotNull
	public static ClassReference computeIfAbsent(@NotNull String name, @NotNull String link) {
		return computeIfAbsent(name, s -> ClassReference.generate(name, link));
	}

	@NotNull
	public static ClassReference computeIfAbsent(@NotNull String name) {
		return computeIfAbsent(name, s -> ClassReference.generate(name, null));
	}

	@Nullable
	public static ClassReference put(@NotNull String name, @NotNull ClassReference classReference) {
		return map.put(getSimpleClassName(name), classReference);
	}

	@Nullable
	public static ClassReference put(@NotNull String name, @Nullable String link) {
		return put(name, ClassReference.generate(name, link));
	}
}
