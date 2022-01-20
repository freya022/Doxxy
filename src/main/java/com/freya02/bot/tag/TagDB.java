package com.freya02.bot.tag;

import com.freya02.bot.db.DBAction;
import com.freya02.bot.db.DBResult;
import com.freya02.bot.db.Database;
import com.freya02.bot.utils.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TagDB {
	private static final int CONTENT_MAX_LENGTH = Message.MAX_CONTENT_LENGTH;
	private static final int DESCRIPTION_MAX_LENGTH = 512;
	private static final int NAME_MAX_LENGTH = OptionData.MAX_CHOICE_NAME_LENGTH;

	private final Database database;

	public TagDB(Database database) throws SQLException {
		this.database = database;

		@Language("PostgreSQL") //IJ moment
		final String setupSql = Utils.readResource("TagDB.sql");

		try (DBAction createTableAction = DBAction.of(database, setupSql)) {createTableAction.executeUpdate();}

		//Setting column restrictions to JDA constants
		//Looks like the driver doesn't understand when we do these 3 alterations in 1 statement
		applyConstraint("alter table Tag add constraint name check(length(name) <= " + NAME_MAX_LENGTH + ")");
		applyConstraint("alter table Tag add constraint description check(length(description) <= " + DESCRIPTION_MAX_LENGTH + ")");
		applyConstraint("alter table Tag add constraint content check(length(content) <= " + CONTENT_MAX_LENGTH + ")");
	}

	private void applyConstraint(@Language("PostgreSQL") String query) throws SQLException {
		try (DBAction constraintAction = DBAction.of(database, query)) {
			constraintAction.executeUpdate();
		}
	}

	private void checkName(String name) {
		if (name.length() > NAME_MAX_LENGTH) {
			throw new TagException("Tag name is too long, it should be under " + NAME_MAX_LENGTH + " characters");
		}
	}

	private void checkDescription(String description) {
		if (description.length() > DESCRIPTION_MAX_LENGTH) {
			throw new TagException("Tag description is too long, it should be under " + DESCRIPTION_MAX_LENGTH + " characters");
		}
	}

	private void checkContent(String content) {
		if (content.length() > CONTENT_MAX_LENGTH) {
			throw new TagException("Tag content is too long, it should be under " + CONTENT_MAX_LENGTH + " characters");
		}
	}

	public void create(long guildId, long ownerId, String name, String description, String content) throws SQLException {
		try (DBAction action = DBAction.of(database, "insert into Tag (guildid, ownerid, name, description, content) " +
				"values (?, ?, ?, ?, ?)")) {

			checkName(name);
			checkDescription(description);
			checkContent(content);

			action.executeUpdate(guildId, ownerId, name, description, content);
		}
	}

	public void edit(long guildId,
	                 long ownerId,
	                 @NotNull String name,
	                 @Nullable String newName,
	                 @Nullable String newDescription,
	                 @Nullable String newContent) throws SQLException {

		if (newName != null) checkName(name);
		if (newDescription != null) checkDescription(newDescription);
		if (newContent != null) checkContent(newContent);

		if (newName != null) {
			try (DBAction action = DBAction.of(database,
					"update Tag set name = ? where guildid = ? and ownerid = ? and name = ?")) {

				action.executeUpdate(newName, guildId, ownerId, name);
			}

			name = newName;
		}

		if (newDescription != null) {
			try (DBAction action = DBAction.of(database,
					"update Tag set description = ? where guildid = ? and ownerid = ? and name = ?")) {

				action.executeUpdate(newDescription, guildId, ownerId, name);
			}
		}

		if (newContent != null) {
			try (DBAction action = DBAction.of(database,
					"update Tag set content = ? where guildid = ? and ownerid = ? and name = ?")) {

				action.executeUpdate(newContent, guildId, ownerId, name);
			}
		}
	}

	public void transfer(long guildId, long ownerId, String name, long newOwnerId) throws SQLException {
		try (DBAction action = DBAction.of(database,
				"update Tag set ownerId = ? where guildid = ? and ownerid = ? and name = ?")) {

			action.executeUpdate(newOwnerId, guildId, ownerId, name);
		}
	}

	public void delete(long guildId, long ownerId, String name) throws SQLException {
		try (DBAction action = DBAction.of(database,
				"delete from Tag where guildid = ? and ownerid = ? and name = ?")) {

			action.executeUpdate(guildId, ownerId, name);
		}
	}

	@Nullable
	public Tag get(long guildId, String name) throws SQLException {
		try (DBAction action = DBAction.of(database,
				"select * from Tag where guildid = ? and name = ?",
				Tag.COLUMN_NAMES)) {

			final DBResult result = action.executeQuery(guildId, name);

			return result.readOnce(Tag::fromResult);
		}
	}

	public void incrementTag(long guildId, String name) throws SQLException {
		try (DBAction action = DBAction.of(database,
				"update Tag set uses = uses + 1 where guildid = ? and name = ?")) {

			action.executeUpdate(guildId, name);
		}
	}

	public int getTotalTags(long guildId) throws SQLException {
		try (DBAction action = DBAction.of(database,
				"select count(*) as totalTags from Tag where guildid = ?",
				"totalTags")) { //Can't use column index on autogenerated values

			final DBResult result = action.executeQuery(guildId);

			final ResultSet set = result.readOnce();
			if (set == null) throw new IllegalStateException();

			return set.getInt("totalTags");
		}
	}

	public List<Tag> getTagRange(long guildId, TagCriteria criteria, int offset, int amount) throws SQLException {
		try (DBAction action = DBAction.of(database,
				"select * from Tag " +
						"where guildid = ? " +
						"order by " + criteria.getKey() + " " +
						"offset ? " +
						"limit ?",
				Tag.COLUMN_NAMES)) {

			final DBResult result = action.executeQuery(guildId, offset, amount);

			return result.transformEach(Tag::fromResult);
		}
	}

	public List<ShortTag> getShortTagsSorted(long guildId, TagCriteria criteria) throws SQLException {
		try (DBAction action = DBAction.of(database,
				"select name, description from Tag " +
						"where guildid = ? " +
						"order by " + criteria.getKey(),
				ShortTag.COLUMN_NAMES)) {

			final DBResult result = action.executeQuery(guildId);

			return result.transformEach(ShortTag::fromResult);
		}
	}

	public List<ShortTag> getShortTagsSorted(long guildId, long ownerId, TagCriteria criteria) throws SQLException {
		try (DBAction action = DBAction.of(database,
				"select name, description from Tag " +
						"where guildid = ? and ownerid = ? " +
						"order by " + criteria.getKey(),
				ShortTag.COLUMN_NAMES)) {

			final DBResult result = action.executeQuery(guildId, ownerId);

			return result.transformEach(ShortTag::fromResult);
		}
	}

	public long getRank(long guildId, String name) throws SQLException {
		try (DBAction action = DBAction.of(database,
				"select rank from " +
						"(select name, dense_rank() over (order by uses desc) as rank from Tag " +
						"where guildid = ?) as ranks " +
						"where name = ?",
				"rank")) { //Can't use column index on autogenerated values

			final DBResult result = action.executeQuery(guildId, name);

			final ResultSet set = result.readOnce();
			if (set == null) throw new IllegalStateException();

			return set.getLong(1);
		}
	}
}
