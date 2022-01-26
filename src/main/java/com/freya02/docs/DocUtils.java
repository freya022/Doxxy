package com.freya02.docs;

import com.freya02.bot.utils.DecomposedName;
import org.jetbrains.annotations.NotNull;

import java.util.StringJoiner;

public class DocUtils {
	@NotNull
	public static String getSimpleSignature(@NotNull String elementId) {
		final StringBuilder simpleSignatureBuilder = new StringBuilder();

		final int index = elementId.indexOf('(');
		simpleSignatureBuilder.append(elementId, 0, index);

		final StringJoiner parameterJoiner = new StringJoiner(", ", "(", ")");
		final String[] parameters = elementId.substring(index + 1, elementId.length() - 1).split(",");
		for (String parameter : parameters) {
			if (parameter.isBlank()) continue;

			final String className = DecomposedName.getSimpleClassName(parameter.trim());

			parameterJoiner.add(className);
		}

		simpleSignatureBuilder.append(parameterJoiner);

		return simpleSignatureBuilder.toString();
	}
}
