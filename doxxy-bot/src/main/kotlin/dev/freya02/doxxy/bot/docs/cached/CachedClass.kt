package dev.freya02.doxxy.bot.docs.cached

import dev.freya02.doxxy.bot.docs.index.DocIndex
import dev.freya02.doxxy.bot.docs.metadata.ImplementationIndex
import dev.freya02.doxxy.docs.sections.SeeAlso.SeeAlsoReference
import net.dv8tion.jda.api.entities.MessageEmbed

class CachedClass(
    override val docIndex: DocIndex,
    val name: String,
    override val embed: MessageEmbed,
    override val seeAlsoReferences: List<SeeAlsoReference>,
    override val sourceLink: String?,
    val subclasses: List<ImplementationIndex.Class>,
    val superclasses: List<ImplementationIndex.Class>
) : CachedDoc {
    override val qualifiedName: String
        get() = name
}
