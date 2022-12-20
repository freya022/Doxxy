package com.freya02.bot.commands.slash

import com.freya02.bot.format.Formatter
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.CommandScope
import com.freya02.botcommands.api.commands.application.slash.GlobalSlashEvent
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import com.freya02.botcommands.api.modals.Modals
import com.freya02.botcommands.api.modals.annotations.ModalHandler
import com.freya02.botcommands.api.modals.annotations.ModalInput
import com.freya02.botcommands.api.modals.create
import com.freya02.botcommands.api.modals.paragraphTextInput
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

private const val FORMAT_MODAL_HANDLER_NAME = "SlashFormat: formatModal"
private const val FORMAT_MODAL_CODE_INPUT_NAME = "SlashFormat: formatModalCodeInput"

@CommandMarker
class SlashFormat : ApplicationCommand() {
    @JDASlashCommand(
        scope = CommandScope.GLOBAL,
        name = "format",
        description = "Formats your code and sends it back to you as a copy-paste-able block"
    )
    fun onSlashFormat(event: GlobalSlashEvent, modals: Modals) {
        val modal = modals.create("Format code") {
            paragraphTextInput(FORMAT_MODAL_CODE_INPUT_NAME, "Code to format")

            bindTo(FORMAT_MODAL_HANDLER_NAME)
        }

        event.replyModal(modal).queue()
    }

    @ModalHandler(name = FORMAT_MODAL_HANDLER_NAME)
    fun onFormatModalSubmit(
        event: ModalInteractionEvent,
        @ModalInput(name = FORMAT_MODAL_CODE_INPUT_NAME) code: String
    ) {
        val formattedSource =
            Formatter.format(code)?.let {
                """
\`\`\`java
$it
\`\`\`"""
            } ?: "Cannot format this code, this formatter only supports Java, your snippet may have a syntax error."

        if (formattedSource.length > Message.MAX_CONTENT_LENGTH) {
            event.reply_("The formatted code is too large, please use a paste service.", ephemeral = true).queue()
        } else {
            event.reply_(formattedSource, ephemeral = true).queue()
        }
    }
}