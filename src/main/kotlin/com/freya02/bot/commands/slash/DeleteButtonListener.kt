package com.freya02.bot.commands.slash

import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.components.InteractionConstraints
import com.freya02.botcommands.api.components.annotations.JDAButtonListener
import com.freya02.botcommands.api.components.event.ButtonEvent
import com.freya02.botcommands.api.utils.EmojiUtils
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.interactions.components.buttons.Button

class DeleteButtonListener {
    @JDAButtonListener(name = DELETE_MESSAGE_BUTTON_LISTENER_NAME)
    fun onDeleteMessageClick(event: ButtonEvent) {
        event.deferEdit().queue()
        event.message.delete().queue()
    }

    companion object {
        private const val DELETE_MESSAGE_BUTTON_LISTENER_NAME = "DeleteButtonListener: deleteMessage"
        private val WASTEBASKET = EmojiUtils.resolveJDAEmoji("wastebasket")

        fun getDeleteButton(allowedUser: UserSnowflake): Button {
            return Components.dangerButton(DELETE_MESSAGE_BUTTON_LISTENER_NAME)
                .setConstraints(InteractionConstraints.ofUserIds(allowedUser.idLong).addPermissions(Permission.MESSAGE_MANAGE))
                .oneUse()
                .build(WASTEBASKET)
        }
    }
}