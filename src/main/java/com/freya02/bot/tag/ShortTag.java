package com.freya02.bot.tag;

import java.lang.reflect.RecordComponent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public record ShortTag(String name, String description) {
	public static final String[] COLUMN_NAMES = Arrays.stream(ShortTag.class.getRecordComponents())
			.map(RecordComponent::getName)
			.toArray(String[]::new);

	public static ShortTag fromResult(ResultSet set) throws SQLException {
		return new ShortTag(
				set.getString("name"),
				set.getString("description")
		);
	}
}
