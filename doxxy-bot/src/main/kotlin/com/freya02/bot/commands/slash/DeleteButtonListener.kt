package com.freya02.bot.commands.slash

import com.freya02.bot.utils.componentIds
import io.github.freya022.botcommands.api.components.Buttons
import io.github.freya022.botcommands.api.components.Components
import io.github.freya022.botcommands.api.components.annotations.JDAButtonListener
import io.github.freya022.botcommands.api.components.event.ButtonEvent
import io.github.freya022.botcommands.api.core.annotations.Handler
import io.github.freya022.botcommands.api.utils.EmojiUtils
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.interactions.components.buttons.Button

@Handler
class DeleteButtonListener {
    @JDAButtonListener(name = DELETE_MESSAGE_BUTTON_LISTENER_NAME)
    suspend fun onDeleteMessageClick(event: ButtonEvent, components: Components) {
        event.deferEdit().queue()
        event.hook.deleteOriginal().queue()
        components.deleteComponentsByIds(event.message.componentIds)
    }

    companion object {
        private const val DELETE_MESSAGE_BUTTON_LISTENER_NAME = "DeleteButtonListener: deleteMessage"
        private val WASTEBASKET = EmojiUtils.resolveJDAEmoji("wastebasket")

        suspend fun Buttons.messageDelete(allowedUser: UserSnowflake): Button {
            return danger(WASTEBASKET).persistent {
                oneUse = true
                bindTo(DELETE_MESSAGE_BUTTON_LISTENER_NAME)

                constraints += allowedUser
                constraints += Permission.MESSAGE_MANAGE
            }
        }
    }
}
