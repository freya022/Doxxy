package com.freya02.bot.docs.cached

import com.freya02.docs.data.SeeAlso.SeeAlsoReference
import net.dv8tion.jda.api.entities.MessageEmbed

interface CachedDoc {
    val embed: MessageEmbed
    val seeAlsoReferences: List<SeeAlsoReference>
    val javadocLink: String?
    val sourceLink: String?
}