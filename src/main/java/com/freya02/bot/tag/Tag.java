package com.freya02.bot.tag;

import java.sql.ResultSet;
import java.sql.SQLException;

public record Tag(long guildId, long ownerId, String name, String text, int uses) {
	public static Tag fromResult(ResultSet set) throws SQLException {
		return new Tag(
				set.getLong("guildId"),
				set.getLong("ownerId"),
				set.getString("name"),
				set.getString("text"),
				set.getInt("uses")
		);
	}
}