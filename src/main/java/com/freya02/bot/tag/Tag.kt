package com.freya02.bot.tag;

import java.lang.reflect.RecordComponent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;

public record Tag(long guildId, long ownerId, Instant createdAt, String name, String description, String content, int uses) {
	public static final String[] COLUMN_NAMES = Arrays.stream(Tag.class.getRecordComponents())
			.map(RecordComponent::getName)
			.toArray(String[]::new);

	public static Tag fromResult(ResultSet set) throws SQLException {
		return new Tag(
				set.getLong("guildId"),
				set.getLong("ownerId"),
				set.getTimestamp("createdAt").toInstant(),
				set.getString("name"),
				set.getString("description"),
				set.getString("content"),
				set.getInt("uses")
		);
	}
}