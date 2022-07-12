package com.freya02.bot.tag;

public class TagException extends RuntimeException {
	public TagException(String message) {
		super(message, null, true, false);
	}
}
