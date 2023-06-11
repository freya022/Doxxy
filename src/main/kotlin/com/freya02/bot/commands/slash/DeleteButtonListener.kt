package com.freya02.bot.commands.slash

import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.components.annotations.JDAButtonListener
import com.freya02.botcommands.api.components.event.ButtonEvent
import com.freya02.botcommands.api.core.annotations.Handler
import com.freya02.botcommands.api.utils.EmojiUtils
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

@Handler
class DeleteButtonListener {
    @JDAButtonListener(name = DELETE_MESSAGE_BUTTON_LISTENER_NAME)
    suspend fun onDeleteMessageClick(event: ButtonEvent, components: Components) {
        event.deferEdit().queue()
        event.hook.deleteOriginal().queue()
        components.deleteComponentsById(event.message.components.flatMap { it.actionComponents }.mapNotNull { it.id })
    }

    companion object {
        private const val DELETE_MESSAGE_BUTTON_LISTENER_NAME = "DeleteButtonListener: deleteMessage"
        private val WASTEBASKET = EmojiUtils.resolveJDAEmoji("wastebasket")

        fun Components.messageDeleteButton(allowedUser: UserSnowflake): Button {
            return persistentButton(ButtonStyle.DANGER, emoji = WASTEBASKET) {
                oneUse = true
                bindTo(DELETE_MESSAGE_BUTTON_LISTENER_NAME)

                constraints += allowedUser
                constraints += Permission.MESSAGE_MANAGE
            }
        }
    }
}
