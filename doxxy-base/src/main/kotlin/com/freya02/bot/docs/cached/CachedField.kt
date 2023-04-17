package com.freya02.bot.docs.cached

import com.freya02.docs.DocSourceType
import com.freya02.docs.data.SeeAlso.SeeAlsoReference
import net.dv8tion.jda.api.entities.MessageEmbed

class CachedField(
    override val source: DocSourceType,
    override val embed: MessageEmbed,
    override val seeAlsoReferences: List<SeeAlsoReference>,
    override val javadocLink: String?,
    override val sourceLink: String?
) : CachedDoc