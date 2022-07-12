package com.freya02.bot.tag;

public enum TagCriteria {
	NAME("name"),
	USES("uses desc");

	private final String key;

	TagCriteria(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}
}
