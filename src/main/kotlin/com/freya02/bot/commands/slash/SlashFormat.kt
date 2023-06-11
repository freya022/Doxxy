package com.freya02.bot.commands.slash

import com.freya02.bot.format.Formatter
import com.freya02.bot.format.FormattingException
import com.freya02.botcommands.api.commands.annotations.Command
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.CommandScope
import com.freya02.botcommands.api.commands.application.slash.GlobalSlashEvent
import com.freya02.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import com.freya02.botcommands.api.modals.Modals
import com.freya02.botcommands.api.modals.create
import com.freya02.botcommands.api.modals.paragraphTextInput
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.entities.Message

@Command
class SlashFormat : ApplicationCommand() {
    @JDASlashCommand(
        scope = CommandScope.GLOBAL,
        name = "format",
        description = "Formats your code and sends it back to you as a copy-paste-able block"
    )
    suspend fun onSlashFormat(event: GlobalSlashEvent, modals: Modals) {
        val modal = modals.create("Format code") {
            paragraphTextInput("code", "Code to format") {
                id = "code"
            }
        }

        event.replyModal(modal).queue()

        val modalEvent = modal.await()
        val code = modalEvent.getValue("code")!!.asString

        try {
            val formattedSource =
                """
                    \`\`\`java
                    ${Formatter.format(code)}
                    \`\`\`
                """.trimIndent()

            if (formattedSource.length > Message.MAX_CONTENT_LENGTH) {
                modalEvent.reply_("The formatted code is too large, please use a paste service.", ephemeral = true).queue()
            } else {
                modalEvent.reply_(formattedSource, ephemeral = true).queue()
            }
        } catch (e: FormattingException) {
            modalEvent.reply_("Cannot format this code, this formatter only supports Java, your snippet may have a syntax error.", ephemeral = true).queue()
        }
    }
}