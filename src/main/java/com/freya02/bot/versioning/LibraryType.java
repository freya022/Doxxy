package com.freya02.bot.versioning;

public enum LibraryType {
	JDA4("JDA 4"),
	JDA5("JDA 5"),
	BOT_COMMANDS("BotCommands");

	private final String displayString;

	LibraryType(String displayString) {
		this.displayString = displayString;
	}

	public String getDisplayString() {
		return displayString;
	}
}
