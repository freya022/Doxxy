package com.freya02.bot.docs.cached

import com.freya02.docs.data.SeeAlso.SeeAlsoReference
import net.dv8tion.jda.api.entities.MessageEmbed

class CachedMethod(
    override val embed: MessageEmbed,
    override val seeAlsoReferences: List<SeeAlsoReference>,
    override val link: String?
) : CachedDoc