package com.freya02.bot.tag;

import com.freya02.bot.Database;
import com.freya02.bot.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class TagDB {
	private final Database database;

	public TagDB(Database database) throws SQLException {
		this.database = database;

		final String setupSql = Utils.readResource("TagDB.sql");
		try (Connection connection = database.getConnection()) {
			connection.prepareStatement(setupSql).execute();
		}
	}

	public void create(long guildId, long ownerId, String name, String text) throws SQLException {
		try (Connection connection = database.getConnection()) {
			final PreparedStatement statement = connection.prepareStatement("insert into Tag (guildid, ownerid, name, text) values (?, ?, ?, ?)");

			statement.setLong(1, guildId);
			statement.setLong(2, ownerId);
			statement.setString(3, name);
			statement.setString(4, text);

			statement.executeUpdate();
		}
	}

	public void edit(long guildId, long ownerId, String name, String text) throws SQLException {
		try (Connection connection = database.getConnection()) {
			final PreparedStatement statement = connection.prepareStatement("update Tag set text = ? where guildid = ? and ownerid = ? and name = ?");

			statement.setString(1, text);
			statement.setLong(2, guildId);
			statement.setLong(3, ownerId);
			statement.setString(4, name);

			statement.executeUpdate();
		}
	}

	public void delete(long guildId, long ownerId, String name) throws SQLException {
		try (Connection connection = database.getConnection()) {
			final PreparedStatement statement = connection.prepareStatement("delete from Tag where guildid = ? and ownerid = ? and name = ?");

			statement.setLong(1, guildId);
			statement.setLong(2, ownerId);
			statement.setString(3, name);

			statement.executeUpdate();
		}
	}

	@Nullable
	public Tag get(long guildId, String name) throws SQLException {
		try (Connection connection = database.getConnection()) {
			final PreparedStatement statement = connection.prepareStatement("select * from Tag where guildid = ? and name = ?");

			statement.setLong(1, guildId);
			statement.setString(2, name);

			final ResultSet set = statement.executeQuery();

			if (!set.next()) return null;

			return Tag.fromResult(set);
		}
	}

	public void incrementTag(long guildId, String name) throws SQLException {
		try (Connection connection = database.getConnection()) {
			final PreparedStatement statement = connection.prepareStatement("update Tag set uses = uses + 1 where guildid = ? and name = ?");

			statement.setLong(1, guildId);
			statement.setString(2, name);

			statement.executeUpdate();
		}
	}

	@NotNull
	private List<String> readTagNames(PreparedStatement statement) throws SQLException {
		final ArrayList<String> list = new ArrayList<>();
		final ResultSet set = statement.executeQuery();

		while (set.next()) {
			list.add(set.getString("name"));
		}

		return list;
	}

	public int getTotalTags(long guildId) throws SQLException {
		try (Connection connection = database.getConnection()) {
			final PreparedStatement statement = connection.prepareStatement("select count(*) from Tag where guildid = ?");

			statement.setLong(1, guildId);

			final ResultSet totalSet = statement.executeQuery();
			totalSet.next();

			return totalSet.getInt(1);
		}
	}

	public List<Tag> getTagRange(long guildId, TagCriteria criteria, int offset, int amount) throws SQLException {
		try (Connection connection = database.getConnection()) {
			final PreparedStatement statement = connection.prepareStatement("select * from Tag where guildid = ? order by " + criteria.getKey() + " offset ? limit ?");

			statement.setLong(1, guildId);
			statement.setInt(2, offset);
			statement.setInt(3, amount);

			final List<Tag> list = new ArrayList<>();
			final ResultSet set = statement.executeQuery();

			while (set.next()) {
				list.add(Tag.fromResult(set));
			}

			return list;
		}
	}

	public List<String> getAllNamesSorted(long guildId, TagCriteria criteria) throws SQLException {
		try (Connection connection = database.getConnection()) {
			final PreparedStatement statement = connection.prepareStatement("select name from Tag where guildid = ? order by " + criteria.getKey());
			statement.setLong(1, guildId);

			return readTagNames(statement);
		}
	}

	public List<String> getAllNames(long guildId, long ownerId, TagCriteria criteria) throws SQLException {
		try (Connection connection = database.getConnection()) {
			final PreparedStatement statement = connection.prepareStatement("select name from Tag where guildid = ? and ownerid = ? order by " + criteria.getKey());
			statement.setLong(1, guildId);
			statement.setLong(2, ownerId);

			return readTagNames(statement);
		}
	}

	public long getRank(long guildId, String name) throws SQLException {
		try (Connection connection = database.getConnection()) {
			final PreparedStatement statement = connection.prepareStatement("select rank from (select name, dense_rank() over (order by uses desc) as rank from Tag where guildid = ?) as ranks where name = ?");
			statement.setLong(1, guildId);
			statement.setString(2, name);

			final ResultSet set = statement.executeQuery();
			if (!set.next()) throw new NoSuchElementException();

			return set.getLong("rank");
		}
	}
}
