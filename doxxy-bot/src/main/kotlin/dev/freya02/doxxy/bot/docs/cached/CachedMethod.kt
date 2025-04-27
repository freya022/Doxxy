package dev.freya02.doxxy.bot.docs.cached

import dev.freya02.doxxy.bot.docs.index.DocIndex
import dev.freya02.doxxy.bot.docs.metadata.ImplementationIndex
import dev.freya02.doxxy.docs.data.SeeAlso.SeeAlsoReference
import net.dv8tion.jda.api.entities.MessageEmbed

class CachedMethod(
    override val docIndex: DocIndex,
    val className: String,
    val signature: String,
    override val embed: MessageEmbed,
    override val seeAlsoReferences: List<SeeAlsoReference>,
    override val javadocLink: String?,
    override val sourceLink: String?,
    val implementations: List<ImplementationIndex.Method>,
    val overriddenMethods: List<ImplementationIndex.Method>
) : CachedDoc {
    override val qualifiedName: String
        get() = "$className#$signature"
}