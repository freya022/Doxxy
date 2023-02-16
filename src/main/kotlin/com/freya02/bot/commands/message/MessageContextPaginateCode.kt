package com.freya02.bot.commands.message

import com.freya02.bot.format.Formatter
import com.freya02.bot.format.FormattingException
import com.freya02.bot.pagination.CodePaginator
import com.freya02.bot.pagination.CodePaginatorBuilder
import com.freya02.bot.utils.ParsingUtils.codeBlockRegex
import com.freya02.bot.utils.Utils.digitAmount
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
import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.StringLiteralExpr
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.asDisabled
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.send
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

        var canReplaceStrings = true
        var canUseFormatting = true

        var blocks: List<String> = regenerateBlocks()
            private set

        private fun regenerateBlocks() = buildList {
            val builder = StringBuilder()

            originalContent
                .letIf(canReplaceStrings && replaceStrings) { replaceStrings(it) }
                .letIf(canUseFormatting && useFormatting) { Formatter.format(it) }
                .lines()
                .also { lines ->
                    lines.forEachIndexed { index, line ->
                        val toBeAppended = buildString(line.length + 10) {
                            if (showLineNumbers) {
                                append("${index + 1}".padEnd(lines.size.digitAmount))
                                append(" ")
                            }
                            appendLine(line)
                        }

                        if (builder.length + toBeAppended.length + codeBlockLength > Message.MAX_CONTENT_LENGTH) {
                            add(builder.toString())
                            builder.clear()
                        }

                        builder.append(toBeAppended)
                    }.also { add(builder.toString()) }
                }
        }.also {
            paginator.maxPages = it.size
            paginator.page = 0
        }

        private fun replaceStrings(content: String): String {
            val strings: List<String> = tryParseCode(content).findAll(StringLiteralExpr::class.java)
                .map { it.asString() }.distinct()

            return strings.foldIndexed(content) { i, acc, string ->
                acc.replace(string, "str$i")
            }
        }

        private fun tryParseCode(content: String): Node {
            return try {
                StaticJavaParser.parseBlock("""{ $content }""")
            } catch (e: ParseProblemException) {
                StaticJavaParser.parseBodyDeclaration("""class X { $content }""")
            }
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

        components.addComponents(makeLineNumbersButton(state), makeUseFormattingButton(state), makeReplaceStringsButton(state))

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
                try {
                    state.useFormatting = !state.useFormatting
                } catch (e: FormattingException) {
                    state.canUseFormatting = false
                    buttonEvent.hook.send("Sorry, this code could not be formatted", ephemeral = true).queue()
                } finally {
                    sendCodePaginator(buttonEvent.hook, state)
                }
            }
        }
    }

    private fun makeReplaceStringsButton(state: PaginationState): Button {
        val prefix = if (state.replaceStrings) "Restore" else "Shorten"
        return componentsService.ephemeralButton(ButtonStyle.SECONDARY, "$prefix strings") {
            constraints += state.owner
            bindTo { buttonEvent ->
                buttonEvent.editComponents(buttonEvent.message.components.asDisabled()).queue()
                try {
                    state.replaceStrings = !state.replaceStrings
                } catch (e: ParseProblemException) {
                    state.canReplaceStrings = false
                    buttonEvent.hook.send("Sorry, this code could not be parsed", ephemeral = true).queue()
                } finally {
                    sendCodePaginator(buttonEvent.hook, state)
                }
            }
        }
    }

    private suspend fun withCodeContent(event: IReplyCallback, message: Message, block: suspend (String) -> Unit) {
        val content = suppressContentWarning {
            if (message.attachments.size == 1) {
                return@suppressContentWarning message.attachments.single()
                    .proxy.download().await().use { it.readAllBytes().decodeToString() }
            }

            val codeBlocks = codeBlockRegex.findAll(message.contentRaw).map { it.groupValues[1] }.toList()
            if (codeBlocks.size == 1) {
                return@suppressContentWarning codeBlocks.single()
            }

            return event.hook.send("There is must be 1 attachment or 1 code block in this message", ephemeral = true).queue()
        }

        block(content.trim())
    }
}