package dev.freya02.doxxy.bot.docs.cached

import dev.freya02.doxxy.bot.docs.index.DocIndex
import dev.freya02.doxxy.docs.sections.SeeAlso.SeeAlsoReference
import net.dv8tion.jda.api.entities.MessageEmbed

class CachedField(
    override val docIndex: DocIndex,
    val className: String,
    val fieldName: String,
    override val embed: MessageEmbed,
    override val seeAlsoReferences: List<SeeAlsoReference>,
    override val javadocLink: String?,
    override val sourceLink: String?
) : CachedDoc {
    override val qualifiedName: String
        get() = "$className#$fieldName"
}