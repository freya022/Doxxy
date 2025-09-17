package dev.freya02.doxxy.bot.tag

import java.sql.ResultSet
import java.sql.SQLException

class ShortTag(val name: String, val description: String) {
    companion object {

        @Throws(SQLException::class)
        fun fromResult(set: ResultSet): ShortTag {
            return ShortTag(
                set.getString("name"),
                set.getString("description")
            )
        }
    }
}
