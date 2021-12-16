package com.freya02.bot;

import java.sql.SQLException;

public class SQLCodes {
	public static final String UNIQUE_VIOLATION = "23505";

	public static boolean isUniqueViolation(SQLException e) {
		return e.getSQLState().equals(SQLCodes.UNIQUE_VIOLATION);
	}
}
