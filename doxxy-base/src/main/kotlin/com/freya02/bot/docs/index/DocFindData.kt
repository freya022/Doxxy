package com.freya02.bot.docs.index

import net.dv8tion.jda.api.entities.MessageEmbed

data class DocFindData(val docId: Int, val embed: MessageEmbed, val javadocLink: String?, val sourceLink: String?)