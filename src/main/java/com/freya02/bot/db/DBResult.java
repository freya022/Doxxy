package com.freya02.bot.db;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DBResult implements Iterable<ResultSet> {
	private final ResultSet resultSet;

	DBResult(@NotNull ResultSet resultSet) {
		this.resultSet = resultSet;
	}

	@NotNull
	public ResultSet getResultSet() {
		return resultSet;
	}

	@NotNull
	@Override
	public Iterator<ResultSet> iterator() {
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				try {
					return resultSet.next();
				} catch (SQLException e) {
					throw new RuntimeException("Unable to iterate the result set", e);
				}
			}

			@Override
			public ResultSet next() {
				return resultSet;
			}
		};
	}

	@Nullable
	public ResultSet readOnce() throws SQLException {
		if (!resultSet.next()) {
			return null;
		}

		return resultSet;
	}

	@Nullable
	public <R> R readOnce(ResultFunction<R> resultFunction) throws SQLException {
		if (!resultSet.next()) {
			return null;
		}

		return resultFunction.apply(resultSet);
	}

	public <R> List<R> transformEach(ResultFunction<R> resultFunction) throws SQLException {
		final List<R> list = new ArrayList<>();

		for (ResultSet set : this) {
			list.add(resultFunction.apply(set));
		}

		return list;
	}
}
