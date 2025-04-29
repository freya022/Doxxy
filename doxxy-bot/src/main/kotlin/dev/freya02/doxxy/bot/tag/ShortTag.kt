package dev.freya02.doxxy.bot.tag

import java.lang.reflect.RecordComponent
import java.sql.ResultSet
import java.sql.SQLException

@JvmRecord
data class ShortTag(val name: String, val description: String) {
    companion object {
        val COLUMN_NAMES = ShortTag::class.java.recordComponents
            .map { obj: RecordComponent -> obj.name }
            .toTypedArray()

        @Throws(SQLException::class)
        fun fromResult(set: ResultSet): ShortTag {
            return ShortTag(
                set.getString("name"),
                set.getString("description")
            )
        }
    }
}