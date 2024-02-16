package com.freya02.bot.utils

import net.dv8tion.jda.api.entities.Message

val Message.componentIds: List<String>
    get() = components.flatMap { it.actionComponents }.mapNotNull { it.id }