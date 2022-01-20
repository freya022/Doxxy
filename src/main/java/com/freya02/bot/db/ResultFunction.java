package com.freya02.bot.db;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultFunction<R> {
	R apply(ResultSet set) throws SQLException;
}
