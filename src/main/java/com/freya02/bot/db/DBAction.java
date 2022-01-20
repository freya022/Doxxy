package com.freya02.bot.db;

import com.freya02.botcommands.api.Logging;
import com.freya02.botcommands.internal.utils.ReflectionUtils;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBAction implements AutoCloseable {
	private static final Logger LOGGER = Logging.getLogger();

	private final Connection connection;
	private final PreparedStatement preparedStatement;
	private final boolean shouldReturnData;

	private DBAction(Connection connection, PreparedStatement preparedStatement, boolean shouldReturnData) {
		this.connection = connection;
		this.preparedStatement = preparedStatement;
		this.shouldReturnData = shouldReturnData;
	}

	public static DBAction of(Database database, @Language("PostgreSQL") String statement) throws SQLException {
		final Connection connection = database.getConnection();

		return new DBAction(connection, connection.prepareStatement(statement), false);
	}

	public static DBAction of(Database database, @Language("PostgreSQL") String statement, int... returnedColumnIndexes) throws SQLException {
		final Connection connection = database.getConnection();

		return new DBAction(connection, connection.prepareStatement(statement, returnedColumnIndexes), true);
	}

	public static DBAction of(Database database, @Language("PostgreSQL") String statement, String... returnedColumnNames) throws SQLException {
		final Connection connection = database.getConnection();

		return new DBAction(connection, connection.prepareStatement(statement, returnedColumnNames), true);
	}

	public DBResult executeQuery(Object... parameters) throws SQLException {
		for (int i = 0; i < parameters.length; i++) {
			preparedStatement.setObject(i + 1, parameters[i]);
		}

		if (!shouldReturnData) {
			LOGGER.warn("Call at {} asks for data to be queried but no column names as been specified, this is just a performance issue", ReflectionUtils.formatCallerMethod());
		}

		return new DBResult(preparedStatement.executeQuery());
	}

	/**
	 * @see PreparedStatement#executeUpdate()
	 */
	public int executeUpdate(Object... parameters) throws SQLException {
		for (int i = 0; i < parameters.length; i++) {
			preparedStatement.setObject(i + 1, parameters[i]);
		}

		return preparedStatement.executeUpdate();
	}

	public Connection getConnection() {
		return connection;
	}

	public PreparedStatement getPreparedStatement() {
		return preparedStatement;
	}

	@Override
	public void close() throws SQLException {
		connection.close();
	}
}
