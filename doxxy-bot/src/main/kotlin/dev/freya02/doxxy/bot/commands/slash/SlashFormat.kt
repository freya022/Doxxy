package dev.freya02.doxxy.bot.commands.slash

import dev.freya02.botcommands.jda.ktx.messages.reply_
import dev.freya02.doxxy.bot.format.Formatter
import dev.freya02.doxxy.bot.format.FormattingException
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.CommandScope
import io.github.freya022.botcommands.api.commands.application.slash.GlobalSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData
import io.github.freya022.botcommands.api.modals.Modals
import io.github.freya022.botcommands.api.modals.create
import io.github.freya022.botcommands.api.modals.paragraphTextInput
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.InteractionContextType

@Command
class SlashFormat : ApplicationCommand() {
    @TopLevelSlashCommandData(scope = CommandScope.GLOBAL, contexts = [InteractionContextType.GUILD, InteractionContextType.BOT_DM])
    @JDASlashCommand(name = "format", description = "Formats your code and sends it back to you as a copy-paste-able block")
    suspend fun onSlashFormat(event: GlobalSlashEvent, modals: Modals) {
        val codeInput: TextInput
        val modal = modals.create("Format code") {
            codeInput = paragraphTextInput("code", "Code to format")
        }

        event.replyModal(modal).queue()

        val modalEvent = modal.await()
        val code = modalEvent[codeInput].asString

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
        } catch (_: FormattingException) {
            modalEvent.reply_("Cannot format this code, this formatter only supports Java, your snippet may have a syntax error.", ephemeral = true).queue()
        }
    }
}
