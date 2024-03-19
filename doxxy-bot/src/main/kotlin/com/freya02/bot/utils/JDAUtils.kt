package com.freya02.bot.utils

import dev.minn.jda.ktx.interactions.components.row
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.components.ActionComponent
import net.dv8tion.jda.api.interactions.components.LayoutComponent

val Message.componentIds: List<String>
    get() = components.flatMap { it.actionComponents }.mapNotNull { it.id }

inline fun List<LayoutComponent>.disableIf(condition: (ActionComponent) -> Boolean) = map { row ->
    row.map {
        if (it is ActionComponent && condition(it)) {
            it.asDisabled()
        } else {
            it
        }
    }.row()
}
