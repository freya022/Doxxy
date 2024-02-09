package com.freya02.bot.docs.cached

import com.freya02.docs.DocSourceType
import com.freya02.docs.data.SeeAlso.SeeAlsoReference
import net.dv8tion.jda.api.entities.MessageEmbed

sealed interface CachedDoc {
    val source: DocSourceType
    val qualifiedName: String
    val embed: MessageEmbed
    val seeAlsoReferences: List<SeeAlsoReference>
    val javadocLink: String?
    val sourceLink: String?
}