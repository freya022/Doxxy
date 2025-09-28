package dev.freya02.doxxy.bot.docs.index

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.utils.data.DataObject
import java.sql.ResultSet

data class DocFindData(val docId: Int, val embed: MessageEmbed, val sourceLink: String?) {

    constructor(result: ResultSet) : this(
        result.getInt("id"),
        result.getString("embed").let(DataObject::fromJson).let(EmbedBuilder::fromData).build(),
        result.getString("source_link"),
    )
}
