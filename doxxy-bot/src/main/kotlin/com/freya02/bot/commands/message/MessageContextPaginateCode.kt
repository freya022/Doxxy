package com.freya02.bot.commands.message

import com.freya02.bot.format.Formatter
import com.freya02.bot.pagination.CodePaginator
import com.freya02.bot.pagination.CodePaginatorBuilder
import com.freya02.bot.utils.ParsingUtils.codeBlockRegex
import com.freya02.bot.utils.Utils.digitAmount
import com.freya02.bot.utils.Utils.letIf
import com.freya02.bot.utils.disableIf
import com.github.javaparser.ParseProblemException
import com.github.javaparser.Position
import com.github.javaparser.Range
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.StringLiteralExpr
import dev.minn.jda.ktx.interactions.components.asDisabled
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.send
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.context.annotations.JDAMessageCommand
import io.github.freya022.botcommands.api.commands.application.context.message.GuildMessageEvent
import io.github.freya022.botcommands.api.components.Button
import io.github.freya022.botcommands.api.components.Buttons
import io.github.freya022.botcommands.api.components.builder.IEphemeralActionableComponent
import io.github.freya022.botcommands.api.components.data.InteractionConstraints
import io.github.freya022.botcommands.api.components.event.ButtonEvent
import io.github.freya022.botcommands.api.core.BContext
import io.github.freya022.botcommands.api.core.utils.edit
import io.github.freya022.botcommands.api.core.utils.suppressContentWarning
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import kotlin.reflect.KMutableProperty0
import kotlin.time.Duration.Companion.minutes

private typealias MessageId = Long

private val logger = KotlinLogging.logger { }
private const val CODE_BLOCK_LENGTH = "```java\n```".length

@Command
class MessageContextPaginateCode(
    private val context: BContext,
    private val buttons: Buttons
) : ApplicationCommand() {
    private class PaginationState(val paginator: CodePaginator, val originalContent: String, val owner: UserSnowflake) {
        var showLineNumbers: Boolean = false
            private set
        var replaceStrings: Boolean = false
            private set
        var useFormatting: Boolean = false
            private set

        var canReplaceStrings = true
            private set
        var canUseFormatting = true
            private set

        var blocks: List<String> = regenerateBlocks()
            private set

        // These returns true if the switch applied correctly
        fun showLineNumbers(showLineNumbers: Boolean): Boolean =
            tryUpdateState(::showLineNumbers, null, showLineNumbers)
        fun replaceStrings(replaceStrings: Boolean): Boolean =
            tryUpdateState(::replaceStrings, ::canReplaceStrings, replaceStrings)
        fun useFormatting(useFormatting: Boolean): Boolean =
            tryUpdateState(::useFormatting, ::canUseFormatting, useFormatting)

        // This is actually different from Delegates.observable,
        // as an exception happening on the listener would not roll back the property value
        private fun <T> tryUpdateState(featureProperty: KMutableProperty0<T>, capabilityProperty: KMutableProperty0<Boolean>?, newValue: T): Boolean {
            val oldValue = featureProperty.get()
            return try {
                featureProperty.set(newValue)
                blocks = regenerateBlocks()
                true
            } catch (e: Exception) {
                logger.debug(e) { "Could not update pagination state '${featureProperty.name}'" }
                featureProperty.set(oldValue)
                capabilityProperty?.set(false)
                false
            }
        }

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

                        if (builder.length + toBeAppended.length + CODE_BLOCK_LENGTH > Message.MAX_CONTENT_LENGTH) {
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
            fun Position.toIndexIn(s: String): Int {
                val offset = s.lineSequence().take(line - 1).sumOf { it.length + 1 }
                return offset + column - 1
            }

            fun Range.toKotlinRangeIn(s: String): IntRange {
                return begin.toIndexIn(s) ..< end.toIndexIn(s)
            }

            // Get ranges from farthest to closest, associate the same index to each range with the same string content
            var index = 0
            val indexByContent: MutableMap<String, Int> = hashMapOf()
            // Reverse order allows us to modify the content of the string without affecting the start positions
            val indexByRange: MutableMap<IntRange, Int> = sortedMapOf(Comparator.comparing { range: IntRange -> range.first }.reversed())

            tryParseCode(content)
                .findAll(StringLiteralExpr::class.java)
                .forEach {
                    // Get the index of the string literal, same strings get the same index
                    val contentCount = indexByContent.getOrPut(it.asString()) { index++ }
                    // Associate the range with the index of the string,
                    // this way we have the same index at different places
                    indexByRange[it.range.get().toKotlinRangeIn(content)] = contentCount
                }

            // Replace the ranges by their index
            val builder = StringBuilder(content)
            indexByRange.forEach { (range, index) ->
                // Small adjustments so we edit the string without the quotes
                builder.replace(range.first + 1, range.last /* inclusive */ + 1, "str$index")
            }

            return builder.toString()
        }

        private fun tryParseCode(content: String): Node {
            return try {
                StaticJavaParser.parseBlock("""{ $content }""")
            } catch (e: ParseProblemException) {
                try {
                    StaticJavaParser.parseBodyDeclaration("""class X { $content }""")
                } catch (e: ParseProblemException) {
                    try {
                        StaticJavaParser.parseBodyDeclaration(content)
                    } catch (e: ParseProblemException) {
                        StaticJavaParser.parseBodyDeclaration("""$content }""") //Missing curly bracket
                    }
                }
            }
        }
    }

    private val emptyEmbed = Embed { description = "dummy" }

    @JDAMessageCommand(name = "Paginate code")
    suspend fun onMessageContextPaginateCode(event: GuildMessageEvent) {
        event.deferReply(true).queue()

        withCodeContent(event, event.target) { content ->
            lateinit var paginationState: PaginationState

            val hook = event.hook
            val paginator = CodePaginatorBuilder(context, messageWriter = { onPageChange(paginationState, it, page) })
                .setConstraints(InteractionConstraints.ofUsers(event.user))
                .setTimeout(10.minutes) { instance ->
                    hook.editOriginalComponents().queue()
                    instance.cleanup()
                }
                .build()

            paginationState = PaginationState(paginator, content, event.user)

            sendCodePaginator(event.hook, paginationState)
        }
    }

    private fun onPageChange(state: PaginationState, builder: MessageCreateBuilder, page: Int) = runBlocking {
        val blocks = state.blocks
        builder.addActionRow(makeLineNumbersButton(state), makeUseFormattingButton(state), makeReplaceStringsButton(state))
        builder.setContent("```java\n${blocks[page]}```")
    }

    private fun sendCodePaginator(hook: InteractionHook, state: PaginationState) {
        state.paginator.getCurrentMessage().edit(hook).queue()
    }

    private suspend fun makeLineNumbersButton(state: PaginationState): Button {
        val prefix = if (state.showLineNumbers) "Hide" else "Show"
        return buttons.secondary("$prefix line numbers").ephemeral {
            noTimeout() // Managed by the paginator
            constraints += state.owner
            bindToDebounce { _, hook ->
                state.showLineNumbers(!state.showLineNumbers)
                sendCodePaginator(hook, state)
                true
            }
        }
    }

    private suspend fun makeUseFormattingButton(state: PaginationState): Button {
        val prefix = if (state.useFormatting) "Disable" else "Enable"
        return buttons.secondary("$prefix formatting").ephemeral {
            noTimeout() // Managed by the paginator
            constraints += state.owner
            if (!state.canUseFormatting) return@ephemeral

            bindToDebounce { _, hook ->
                if (state.useFormatting(!state.useFormatting)) {
                    sendCodePaginator(hook, state)
                    true
                } else {
                    hook.send("Sorry, this code could not be formatted", ephemeral = true).queue()
                    false
                }
            }
        }.withDisabled(!state.canUseFormatting)
    }

    private suspend fun makeReplaceStringsButton(state: PaginationState): Button {
        val prefix = if (state.replaceStrings) "Restore" else "Shorten"
        return buttons.secondary("$prefix strings").ephemeral {
            noTimeout() // Managed by the paginator
            constraints += state.owner
            if (!state.canReplaceStrings) return@ephemeral

            bindToDebounce { _, hook ->
                if (state.replaceStrings(!state.replaceStrings)) {
                    sendCodePaginator(hook, state)
                    true
                } else {
                    hook.send("Sorry, this code could not be parsed", ephemeral = true).queue()
                    false
                }
            }
        }.withDisabled(!state.canReplaceStrings)
    }

    /**
     * [block] returns `true` if the message has been updated,
     * `false` if the message needs to have its components bounced (enabled) back.
     */
    private fun IEphemeralActionableComponent<*, ButtonEvent>.bindToDebounce(block: suspend (ButtonEvent, InteractionHook) -> Boolean) = bindTo {
        it.editComponents(it.message.components.asDisabled()).queue()
        try {
            if (!block(it, it.hook)) {
                it.hook.editOriginalComponents(it.message.components.disableIf { c -> c.id == it.componentId }).queue()
            }
        } catch (e: Exception) {
            it.hook.editOriginalComponents(it.message.components).queue()
            throw e
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