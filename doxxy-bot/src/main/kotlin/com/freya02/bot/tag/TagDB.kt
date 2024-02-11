package com.freya02.bot.tag

import io.github.freya022.botcommands.api.core.db.Database
import io.github.freya022.botcommands.api.core.db.preparedStatement
import io.github.freya022.botcommands.api.core.service.annotations.BService
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import java.sql.SQLException

@BService
class TagDB(private val database: Database) {
    private fun checkName(name: String) {
        if (name.length > NAME_MAX_LENGTH)
            throw TagException("Tag name is too long, it should be under $NAME_MAX_LENGTH characters")
        if (name.length < NAME_MIN_LENGTH)
            throw TagException("Tag name is too small, it should be above $NAME_MIN_LENGTH characters")
    }

    private fun checkDescription(description: String) {
        if (description.length > DESCRIPTION_MAX_LENGTH)
            throw TagException("Tag description is too long, it should be under $DESCRIPTION_MAX_LENGTH characters")
        if (description.length < DESCRIPTION_MIN_LENGTH)
            throw TagException("Tag description is too small, it should be above $DESCRIPTION_MIN_LENGTH characters")
    }

    private fun checkContent(content: String) {
        if (content.length > CONTENT_MAX_LENGTH)
            throw TagException("Tag content is too long, it should be under $CONTENT_MAX_LENGTH characters")
        if (content.length < CONTENT_MIN_LENGTH)
            throw TagException("Tag content is too small, it should be above $CONTENT_MIN_LENGTH characters")
    }

    @Throws(SQLException::class)
    suspend fun create(guildId: Long, ownerId: Long, name: String, description: String, content: String) {
        database.preparedStatement("insert into Tag (guildid, ownerid, name, description, content) values (?, ?, ?, ?, ?)") {
            checkName(name)
            checkDescription(description)
            checkContent(content)
            executeUpdate(guildId, ownerId, name, description, content)
        }
    }

    @Throws(SQLException::class)
    suspend fun edit(
        guildId: Long,
        ownerId: Long,
        name: String,
        newName: String,
        newDescription: String,
        newContent: String
    ) {
        checkName(newName)
        checkDescription(newDescription)
        checkContent(newContent)

        database.preparedStatement(
            """
                update Tag
                set name        = coalesce(?, name),
                    description = coalesce(?, description),
                    content     = coalesce(?, content)
                where guildid = ?
                  and ownerid = ?
                  and name = ?""".trimIndent()
        ) {
            executeUpdate(newName, newDescription, newContent, guildId, ownerId, name)
        }
    }

    @Throws(SQLException::class)
    suspend fun transfer(guildId: Long, ownerId: Long, name: String, newOwnerId: Long) {
        database.preparedStatement("update Tag set ownerId = ? where guildid = ? and ownerid = ? and name = ?") {
            executeUpdate(newOwnerId, guildId, ownerId, name)
        }
    }

    @Throws(SQLException::class)
    suspend fun delete(guildId: Long, ownerId: Long, name: String) {
        database.preparedStatement("delete from Tag where guildid = ? and ownerid = ? and name = ?") {
            executeUpdate(guildId, ownerId, name)
        }
    }

    @Throws(SQLException::class)
    suspend fun get(guildId: Long, name: String): Tag? {
        database.preparedStatement("select * from Tag where guildid = ? and name = ?", readOnly = true) {
            val result = executeQuery(guildId, name)
            return result.readOrNull()?.let { Tag.fromResult(it) }
        }
    }

    @Throws(SQLException::class)
    suspend fun incrementTag(guildId: Long, name: String) {
        database.preparedStatement("update Tag set uses = uses + 1 where guildid = ? and name = ?") {
            executeUpdate(guildId, name)
        }
    }

    @Throws(SQLException::class)
    suspend fun getTotalTags(guildId: Long): Int {
        database.preparedStatement("select count(*) as totalTags from Tag where guildid = ?", readOnly = true) {
            return executeQuery(guildId).read().getInt("totalTags")
        }
    }

    @Throws(SQLException::class)
    fun getTagRange(guildId: Long, criteria: TagCriteria, offset: Int, amount: Int): List<Tag> = runBlocking {
        database.preparedStatement("select * from Tag where guildid = ? order by ${criteria.key} offset ? limit ?", readOnly = true) {
            executeQuery(guildId, offset, amount).map(Tag.Companion::fromResult)
        }
    }

    @Throws(SQLException::class)
    fun getShortTagsSorted(guildId: Long, criteria: TagCriteria): List<ShortTag> = runBlocking {
        database.preparedStatement("select name, description from Tag where guildid = ? order by ${criteria.key}", readOnly = true) {
            executeQuery(guildId).map(ShortTag.Companion::fromResult)
        }
    }

    @Throws(SQLException::class)
    fun getShortTagsSorted(guildId: Long, ownerId: Long, criteria: TagCriteria): List<ShortTag> = runBlocking {
        database.preparedStatement("select name, description from Tag where guildid = ? and ownerid = ? order by ${criteria.key}", readOnly = true) {
            executeQuery(guildId, ownerId).map(ShortTag.Companion::fromResult)
        }
    }

    @Throws(SQLException::class)
    suspend fun getRank(guildId: Long, name: String?): Long {
        database.preparedStatement("select rank from (select name, dense_rank() over (order by uses desc) as rank from Tag where guildid = ?) as ranks where name = ?", readOnly = true) {
            return executeQuery(guildId, name).read().getLong(1)
        }
    }

    companion object {
        const val NAME_MIN_LENGTH = 2
        const val NAME_MAX_LENGTH = OptionData.MAX_CHOICE_NAME_LENGTH
        const val DESCRIPTION_MIN_LENGTH = 10
        const val DESCRIPTION_MAX_LENGTH = 512
        const val CONTENT_MIN_LENGTH = 1
        const val CONTENT_MAX_LENGTH = Message.MAX_CONTENT_LENGTH
    }
}