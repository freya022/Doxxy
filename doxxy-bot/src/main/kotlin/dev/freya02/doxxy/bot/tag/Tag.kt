package dev.freya02.doxxy.bot.tag

import java.lang.reflect.RecordComponent
import java.sql.ResultSet
import java.sql.SQLException
import java.time.Instant

@JvmRecord
data class Tag(
    val guildId: Long,
    val ownerId: Long,
    val createdAt: Instant,
    val name: String,
    val description: String,
    val content: String,
    val uses: Int
) {
    companion object {
        val COLUMN_NAMES =
            Tag::class.java.recordComponents
            .map { obj: RecordComponent -> obj.name }
            .toTypedArray()

        @Throws(SQLException::class)
        fun fromResult(set: ResultSet): Tag {
            return Tag(
                set.getLong("guildId"),
                set.getLong("ownerId"),
                set.getTimestamp("createdAt").toInstant(),
                set.getString("name"),
                set.getString("description"),
                set.getString("content"),
                set.getInt("uses")
            )
        }
    }
}