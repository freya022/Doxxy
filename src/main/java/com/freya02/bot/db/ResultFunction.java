package com.freya02.bot.db;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultFunction<R> {
	R apply(@NotNull ResultSet set) throws SQLException;
}
