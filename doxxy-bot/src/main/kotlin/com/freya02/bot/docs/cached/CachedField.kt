package com.freya02.bot.docs.cached

import com.freya02.bot.docs.index.DocIndex
import com.freya02.docs.data.SeeAlso.SeeAlsoReference
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