package com.freya02.bot.tag

import com.freya02.bot.db.DBAction
import com.freya02.bot.db.Database
import com.freya02.bot.utils.Utils.readResource
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.intellij.lang.annotations.Language
import java.sql.ResultSet
import java.sql.SQLException

private const val CONTENT_MAX_LENGTH = Message.MAX_CONTENT_LENGTH
private const val DESCRIPTION_MAX_LENGTH = 512
private const val NAME_MAX_LENGTH = OptionData.MAX_CHOICE_NAME_LENGTH

class TagDB(private val database: Database) {
    init {
        val setupSql = readResource("TagDB.sql")
        DBAction.of(database, setupSql).use { createTableAction -> createTableAction.executeUpdate() }

        //Setting column restrictions to JDA constants
        //Looks like the driver doesn't understand when we do these 3 alterations in 1 statement
        applyConstraint("alter table Tag add constraint name check(length(name) <= $NAME_MAX_LENGTH)")
        applyConstraint("alter table Tag add constraint description check(length(description) <= $DESCRIPTION_MAX_LENGTH)")
        applyConstraint("alter table Tag add constraint content check(length(content) <= $CONTENT_MAX_LENGTH)")
    }

    private fun applyConstraint(@Language("PostgreSQL") query: String) {
        DBAction.of(database, query).use { constraintAction -> constraintAction.executeUpdate() }
    }

    private fun checkName(name: String) {
        if (name.length > NAME_MAX_LENGTH)
            throw TagException("Tag name is too long, it should be under $NAME_MAX_LENGTH characters")
    }

    private fun checkDescription(description: String) {
        if (description.length > DESCRIPTION_MAX_LENGTH)
            throw TagException("Tag description is too long, it should be under $DESCRIPTION_MAX_LENGTH characters")
    }

    private fun checkContent(content: String) {
        if (content.length > CONTENT_MAX_LENGTH)
            throw TagException("Tag content is too long, it should be under $CONTENT_MAX_LENGTH characters")
    }

    @Throws(SQLException::class)
    fun create(guildId: Long, ownerId: Long, name: String, description: String, content: String) {
        DBAction.of(
            database,
            "insert into Tag (guildid, ownerid, name, description, content) values (?, ?, ?, ?, ?)"
        ).use { action ->
            checkName(name)
            checkDescription(description)
            checkContent(content)
            action.executeUpdate(guildId, ownerId, name, description, content)
        }
    }

    @Throws(SQLException::class)
    fun edit(
        guildId: Long,
        ownerId: Long,
        name: String,
        newName: String?,
        newDescription: String?,
        newContent: String?
    ) {
        newName?.let { checkName(it) }
        newDescription?.let { checkDescription(it) }
        newContent?.let { checkContent(it) }

        var name = name
        if (newName != null) {
            DBAction.of(
                database,
                "update Tag set name = ? where guildid = ? and ownerid = ? and name = ?"
            ).use { action -> action.executeUpdate(newName, guildId, ownerId, name) }

            name = newName
        }

        if (newDescription != null) {
            DBAction.of(
                database,
                "update Tag set description = ? where guildid = ? and ownerid = ? and name = ?"
            ).use { action -> action.executeUpdate(newDescription, guildId, ownerId, name) }
        }

        if (newContent != null) {
            DBAction.of(
                database,
                "update Tag set content = ? where guildid = ? and ownerid = ? and name = ?"
            ).use { action -> action.executeUpdate(newContent, guildId, ownerId, name) }
        }
    }

    @Throws(SQLException::class)
    fun transfer(guildId: Long, ownerId: Long, name: String, newOwnerId: Long) {
        DBAction.of(
            database,
            "update Tag set ownerId = ? where guildid = ? and ownerid = ? and name = ?"
        ).use { action -> action.executeUpdate(newOwnerId, guildId, ownerId, name) }
    }

    @Throws(SQLException::class)
    fun delete(guildId: Long, ownerId: Long, name: String) {
        DBAction.of(
            database,
            "delete from Tag where guildid = ? and ownerid = ? and name = ?"
        ).use { action -> action.executeUpdate(guildId, ownerId, name) }
    }

    @Throws(SQLException::class)
    operator fun get(guildId: Long, name: String): Tag? {
        DBAction.of(
            database,
            "select * from Tag where guildid = ? and name = ?",
            *Tag.COLUMN_NAMES
        ).use { action ->
            val result = action.executeQuery(guildId, name)
            return result.readOnce { set: ResultSet? -> Tag.fromResult(set) }
        }
    }

    @Throws(SQLException::class)
    fun incrementTag(guildId: Long, name: String) {
        DBAction.of(
            database,
            "update Tag set uses = uses + 1 where guildid = ? and name = ?"
        ).use { action -> action.executeUpdate(guildId, name) }
    }

    @Throws(SQLException::class)
    fun getTotalTags(guildId: Long): Int {
        DBAction.of(
            database,
            "select count(*) as totalTags from Tag where guildid = ?",
            "totalTags"
        ).use { action ->  //Can't use column index on autogenerated values
            val result = action.executeQuery(guildId)
            val set = result.readOnce() ?: throw IllegalStateException()
            return set.getInt("totalTags")
        }
    }

    @Throws(SQLException::class)
    fun getTagRange(guildId: Long, criteria: TagCriteria, offset: Int, amount: Int): List<Tag> {
        DBAction.of(
            database,
            "select * from Tag where guildid = ? order by ${criteria.key} offset ? limit ?",
            *Tag.COLUMN_NAMES
        ).use { action ->
            val result = action.executeQuery(guildId, offset, amount)
            return result.transformEach { set: ResultSet? -> Tag.fromResult(set) }
        }
    }

    @Throws(SQLException::class)
    fun getShortTagsSorted(guildId: Long, criteria: TagCriteria): List<ShortTag> {
        DBAction.of(
            database,
            "select name, description from Tag where guildid = ? order by ${criteria.key}",
            *ShortTag.COLUMN_NAMES
        ).use { action ->
            val result = action.executeQuery(guildId)
            return result.transformEach { set: ResultSet? -> ShortTag.fromResult(set) }
        }
    }

    @Throws(SQLException::class)
    fun getShortTagsSorted(guildId: Long, ownerId: Long, criteria: TagCriteria): List<ShortTag> {
        DBAction.of(
            database,
            "select name, description from Tag where guildid = ? and ownerid = ? order by ${criteria.key}",
            *ShortTag.COLUMN_NAMES
        ).use { action ->
            val result = action.executeQuery(guildId, ownerId)
            return result.transformEach { set: ResultSet? -> ShortTag.fromResult(set) }
        }
    }

    @Throws(SQLException::class)
    fun getRank(guildId: Long, name: String?): Long {
        DBAction.of(
            database,
            "select rank from (select name, dense_rank() over (order by uses desc) as rank from Tag where guildid = ?) as ranks where name = ?",
            "rank"
        ).use { action ->  //Can't use column index on autogenerated values
            val result = action.executeQuery(guildId, name)
            val set = result.readOnce() ?: throw IllegalStateException()
            return set.getLong(1)
        }
    }
}