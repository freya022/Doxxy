package dev.freya02.doxxy.bot.docs.index

import net.dv8tion.jda.api.entities.MessageEmbed

data class DocFindData(val docId: Int, val embed: MessageEmbed, val sourceLink: String?)
