package com.freya02.bot.commands.message

import com.freya02.bot.format.Formatter
import com.freya02.bot.format.FormattingException
import com.freya02.bot.pagination.CodePaginator
import com.freya02.bot.pagination.CodePaginatorBuilder
import com.freya02.bot.utils.Utils.letIf
import com.freya02.bot.utils.suppressContentWarning
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.context.annotations.JDAMessageCommand
import com.freya02.botcommands.api.commands.application.context.message.GuildMessageEvent
import com.freya02.botcommands.api.components.Button
import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.components.data.InteractionConstraints
import com.freya02.botcommands.api.pagination.PaginatorComponents
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.asDisabled
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

private typealias MessageId = Long

@CommandMarker
class MessageContextPaginateCode(private val componentsService: Components) : ApplicationCommand() {
    private class PaginationState(val paginator: CodePaginator, val originalContent: String, val owner: UserSnowflake) {
        var showLineNumbers: Boolean by Delegates.observable(false) { _, _, _ -> blocks = regenerateBlocks() }
        var replaceStrings: Boolean by Delegates.observable(false) { _, _, _ -> blocks = regenerateBlocks() }
        var useFormatting: Boolean by Delegates.observable(false) { _, _, _ -> blocks = regenerateBlocks() }

        var blocks: List<String> = regenerateBlocks()
            private set

        private fun regenerateBlocks() = buildList {
            val builder = StringBuilder()

            originalContent
                .letIf(useFormatting) { Formatter.format(originalContent) ?: throw FormattingException() } //TODO remove once #format throws it
                .lines()
                .forEachIndexed { index, line ->
                    val toBeAppended = buildString(line.length + 10) {
                        if (showLineNumbers) append("${index + 1}  ")
                        appendLine(line)
                    }

                    if (builder.length + toBeAppended.length + codeBlockLength > Message.MAX_CONTENT_LENGTH) {
                        add(builder.toString())
                        builder.clear()
                    }

                    builder.append(toBeAppended)
                }.also { add(builder.toString()) }
        }.also {
            paginator.maxPages = it.size
            paginator.page = 0
        }

        private companion object {
            private const val codeBlockLength = "```java\n```".length
        }
    }

    private val emptyEmbed = Embed { description = "dummy" }
    private val codeMap: MutableMap<MessageId, PaginationState> = hashMapOf()

    @JDAMessageCommand(name = "Paginate code")
    suspend fun onMessageContextPaginateCode(event: GuildMessageEvent) {
        event.deferReply(true).queue()

        withCodeContent(event, event.target) { content ->
            val hook = event.hook
            val messageId = event.target.idLong
            val paginator = CodePaginatorBuilder(componentsService)
                .setConstraints(InteractionConstraints.ofUsers(event.user))
                .setTimeout(10, TimeUnit.MINUTES) { instance, _ ->
                    codeMap.remove(messageId) //Always executed at some point as there is no delete button
                    hook.editOriginalComponents().queue()
                    instance.cleanup()
                }
                .setPaginatorSupplier { _, editBuilder, components, page ->
                    emptyEmbed.also { onPageChange(messageId, editBuilder, components, page) }
                }
                .build()

            val paginationState = PaginationState(paginator, content, event.user)
            codeMap[messageId] = paginationState

            sendCodePaginator(event.hook, paginationState)
        }
    }

    private fun onPageChange(
        messageId: Long,
        editBuilder: MessageEditBuilder,
        components: PaginatorComponents,
        page: Int
    ) {
        val state = codeMap[messageId]!!
        val blocks = state.blocks

        components.addComponents(makeLineNumbersButton(state), makeUseFormattingButton(state))

        editBuilder.setContent("```java\n${blocks[page]}```")
    }

    private fun sendCodePaginator(hook: InteractionHook, state: PaginationState) {
        val paginator = state.paginator
        hook.editOriginal(paginator.get()).queue()
    }

    private fun makeLineNumbersButton(state: PaginationState): Button {
        val prefix = if (state.showLineNumbers) "Hide" else "Show"
        return componentsService.ephemeralButton(ButtonStyle.SECONDARY, "$prefix line numbers") {
            constraints += state.owner
            bindTo { buttonEvent ->
                buttonEvent.editComponents(buttonEvent.message.components.asDisabled()).queue()
                state.showLineNumbers = !state.showLineNumbers
                sendCodePaginator(buttonEvent.hook, state)
            }
        }
    }

    private fun makeUseFormattingButton(state: PaginationState): Button {
        val prefix = if (state.useFormatting) "Disable" else "Enable"
        return componentsService.ephemeralButton(ButtonStyle.SECONDARY, "$prefix formatting") {
            constraints += state.owner
            bindTo { buttonEvent ->
                buttonEvent.editComponents(buttonEvent.message.components.asDisabled()).queue()
                state.useFormatting = !state.useFormatting
                sendCodePaginator(buttonEvent.hook, state)
            }
        }
    }

    private suspend fun withCodeContent(event: IReplyCallback, message: Message, block: suspend (String) -> Unit) {
        val attachment = suppressContentWarning {
            if (message.attachments.size != 1) {
                return event.reply_("There is must be 1 attachment in this message", ephemeral = true).queue()
            } //TODO support code blocks

            message.attachments.single()
        }

        val content = attachment.proxy.download().await().use { it.readAllBytes().decodeToString() }
        block(content)
    }
}