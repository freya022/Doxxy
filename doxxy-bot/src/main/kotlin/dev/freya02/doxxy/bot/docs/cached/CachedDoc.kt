package dev.freya02.doxxy.bot.docs.cached

import dev.freya02.doxxy.bot.docs.index.DocIndex
import dev.freya02.doxxy.docs.DocSourceType
import dev.freya02.doxxy.docs.data.SeeAlso.SeeAlsoReference
import net.dv8tion.jda.api.entities.MessageEmbed

sealed interface CachedDoc {
    val docIndex: DocIndex
    val source: DocSourceType
        get() = docIndex.sourceType
    val qualifiedName: String
    val embed: MessageEmbed
    val seeAlsoReferences: List<SeeAlsoReference>
    val javadocLink: String?
    val sourceLink: String?
}