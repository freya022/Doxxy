package com.freya02.bot.commands.message

import com.freya02.bot.pagination.CodePaginatorBuilder
import com.freya02.bot.utils.suppressContentWarning
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.application.ApplicationCommand
import com.freya02.botcommands.api.commands.application.context.annotations.JDAMessageCommand
import com.freya02.botcommands.api.commands.application.context.message.GuildMessageEvent
import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.components.data.InteractionConstraints
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import java.util.concurrent.TimeUnit

@CommandMarker
class MessageContextPaginateCode(private val componentsService: Components) : ApplicationCommand() {
    private val codeBlockLength = "```java\n```".length

    @JDAMessageCommand(name = "Paginate code")
    suspend fun onMessageContextPaginateCode(event: GuildMessageEvent) {
        event.deferReply(true).queue()

        val attachment = suppressContentWarning {
            if (event.target.attachments.size != 1) {
                return event.reply_("There is must be 1 attachment in this message", ephemeral = true).queue()
            } //TODO support code blocks

            event.target.attachments.single()
        }

        val blocks: List<String> = attachment.proxy.download().await()
            .use { it.readAllBytes().decodeToString() }
            .let { makeCodeBlocks(it, showLineNumbers = true) }

        CodePaginatorBuilder(componentsService, blocks)
            .setConstraints(InteractionConstraints.ofUsers(event.user))
            .setTimeout(10, TimeUnit.MINUTES) { paginator, _ ->
                event.hook.editOriginalComponents().queue()
                paginator.cleanup()
            }
            .build()
            .get()
            .let { event.hook.sendMessage(MessageCreateData.fromEditData(it)).queue() }
    }

    private fun makeCodeBlocks(content: String, showLineNumbers: Boolean) = buildList {
        val builder = StringBuilder()

        content.lines()
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
    }
}