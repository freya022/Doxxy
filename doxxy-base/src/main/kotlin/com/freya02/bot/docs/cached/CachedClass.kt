package com.freya02.bot.docs.cached

import com.freya02.bot.docs.metadata.ImplementationIndex
import com.freya02.docs.DocSourceType
import com.freya02.docs.data.SeeAlso.SeeAlsoReference
import net.dv8tion.jda.api.entities.MessageEmbed

class CachedClass(
    override val source: DocSourceType,
    val name: String,
    override val embed: MessageEmbed,
    override val seeAlsoReferences: List<SeeAlsoReference>,
    override val javadocLink: String?,
    override val sourceLink: String?,
    val subclasses: List<ImplementationIndex.Class>,
    val superclasses: List<ImplementationIndex.Class>
) : CachedDoc