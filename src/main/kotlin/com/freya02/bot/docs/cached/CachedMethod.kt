package com.freya02.bot.docs.cached

import com.freya02.bot.docs.metadata.ImplementationIndex
import com.freya02.docs.DocSourceType
import com.freya02.docs.data.SeeAlso.SeeAlsoReference
import net.dv8tion.jda.api.entities.MessageEmbed

class CachedMethod(
    override val source: DocSourceType,
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