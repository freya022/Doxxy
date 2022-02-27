package com.freya02.bot.versioning.github;

public record CommitHash(String hash) {
	public String asSha10() {
		return hash.substring(0, 10);
	}
}
