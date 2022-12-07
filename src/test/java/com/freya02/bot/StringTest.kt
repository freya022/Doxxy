package com.freya02.bot;

import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

public class StringTest {
	public static void main(String[] args) {
		String a = "A".repeat(50);
		String b = "ABCD".repeat(256);

		String str = getChoiceName(a, b);
		System.out.println("str = " + str);
		System.out.println("str.length() = " + str.length());

		System.out.println("str = " + getChoiceName("abc", "def"));
		System.out.println("str = " + getChoiceName("abc", "def".repeat(100)));
		System.out.println("str = " + getChoiceName("abc".repeat(100), "def"));
		System.out.println("str = " + getChoiceName("abc".repeat(100), "def".repeat(100)));
	}

	@NotNull
	private static String getChoiceName(String a, String b) {
		if (a.length() + b.length() + 5 > 100) {
			final int min = Math.max(0, OptionData.MAX_CHOICE_NAME_LENGTH - a.length() - 5);

			if (min == 0) {
				b = "";
			} else {
				b = b.substring(0, min);
			}
		}

		if (a.length() > 100) {
			a = a.substring(0, 100);
		}

		int spaces = Math.max(0, OptionData.MAX_CHOICE_NAME_LENGTH - a.length() - b.length());

		return a + " ".repeat(spaces) + b;
	}
}
