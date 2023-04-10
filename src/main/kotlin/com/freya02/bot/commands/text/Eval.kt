package com.freya02.bot.commands.text

import com.freya02.bot.Config
import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.messageDeleteButton
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.commands.annotations.RequireOwner
import com.freya02.botcommands.api.commands.prefixed.BaseCommandEvent
import com.freya02.botcommands.api.commands.prefixed.TextCommand
import com.freya02.botcommands.api.commands.prefixed.annotations.JDATextCommand
import com.freya02.botcommands.api.commands.prefixed.annotations.TextOption
import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.core.annotations.BEventListener
import com.freya02.botcommands.api.core.config.BCoroutineScopesConfig
import com.freya02.botcommands.internal.unreflect
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.getDefaultScope
import dev.minn.jda.ktx.interactions.components.asDisabled
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.MessageCreate
import kotlinx.coroutines.*
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.FileProxy
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.concurrent.Executors
import javax.script.ScriptContext
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings
import javax.script.SimpleScriptContext
import kotlin.time.Duration.Companion.minutes

private typealias MessageId = Long
private typealias OriginalMessageId = MessageId
private typealias ButtonId = String

@CommandMarker
class Eval(
    private val componentsService: Components,
    private val config: Config,
    private val coroutineScopes: BCoroutineScopesConfig
) : TextCommand() {
    private class EvalState(val inputOffset: Int, val replyId: MessageId, val deleteButtonId: ButtonId, val context: ScriptContext, val timeoutJob: Job)

    private val states: MutableMap<OriginalMessageId, EvalState> = hashMapOf()

    @RequireOwner
    @JDATextCommand(name = "eval")
    suspend fun onTextEval(event: BaseCommandEvent, @TextOption input: String) {
        if (event.author.idLong !in config.ownerIds) {
            return event.reply("${event.author.asMention} is not in the sudoers file. This incident will be reported.").queue()
        }

        val bindings = mapOf(
            "scope" to coroutineScopes.textCommandsScope,
            "channel" to event.channel,
            "guild" to event.guild,
            "jda" to event.jda,
            "user" to event.author,
            "author" to event.author,
            "member" to event.member,
            "selfUser" to event.jda.selfUser,
            "selfMember" to event.guild.selfMember
        )

        val context = SimpleScriptContext().apply {
            setBindings(SimpleBindings(bindings), ScriptContext.ENGINE_SCOPE)
        }

        runAndSendEvalMessage(event.message, event.message.contentRaw.indexOf(input), context, input) {
            event.message.reply(it).mentionRepliedUser(false).await()
        }
    }

    @BEventListener
    suspend fun onEditEvent(event: MessageUpdateEvent) {
        //Does not mean the instance expired, as it can be an update of anything else
        val state = states[event.messageIdLong] ?: return
        state.timeoutJob.cancel()

        runAndSendEvalMessage(event.message, state.inputOffset, state.context) {
            event.channel.editMessageById(state.replyId, MessageEditData.fromCreateData(it))
                .mentionRepliedUser(false)
                .await()
        }
    }

    //Listen to delete button being used
    @BEventListener
    fun onButtonEvent(buttonEvent: ButtonInteractionEvent) {
        states.entries
            .firstOrNull { entry -> entry.value.deleteButtonId == buttonEvent.componentId }
            ?.let { states.remove(it.key, it.value) }
    }

    @BEventListener
    fun onDeleteEvent(event: MessageDeleteEvent) {
        val state = states[event.messageIdLong] ?: return
        event.channel.deleteMessageById(state.replyId).queue({}, ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE))
    }

    private fun getCode(input: String): String {
        val extractedCode = codeBlockRegex.matchEntire(input)?.groupValues?.get(1)
            ?: input.trim()

        return buildString {
            defaultImports.forEach { import ->
                appendLine("import $import")
            }
            appendLine()
            appendLine("scope.async { $extractedCode }")
        }
    }

    private suspend fun runAndSendEvalMessage(
        message: Message,
        inputOffset: Int,
        context: ScriptContext,
        //Not foolproof but whatever tbh, I'm not going to erase the command
        input: String = message.contentRaw.substring(inputOffset),
        sendAction: suspend (MessageCreateData) -> Message,
    ) {
        val code = getCode(input)
        val runResult = runCatching {
            withTimeout(1.minutes) {
                (engine.eval(code, context) as Deferred<*>).await()
            }
        }

        val deleteButton = componentsService.messageDeleteButton(message.author)
        val replyMessage = runResult.fold(
            onSuccess = { returnValue ->
                MessageCreate {
                    embed {
                        title = "Kotlin Eval"
                        if (returnValue === Unit) {
                            description = "Code executed without errors"
                        } else {
                            field("Return value", "```\n${returnValue.toString()}```", false)
                        }
                    }

                    components += row(deleteButton)
                }
            },
            onFailure = {
                MessageCreate(
                    "An error occurred while evaluating",
                    files = listOf(FileUpload.fromData(it.stackTraceToDiscordString().encodeToByteArray(), "error.txt")),
                    components = listOf(row(deleteButton))
                )
            }).let { sendAction(it) }

        val originalMessageId = message.idLong
        val timeoutJob = timeoutScope.launch {
            delay(5.minutes)
            states.remove(originalMessageId)

            replyMessage.editMessageComponents(replyMessage.components.asDisabled())
                .queue({}, ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE))
        }
        states[originalMessageId] = EvalState(inputOffset, replyMessage.idLong, deleteButton.id!!, context, timeoutJob)
    }

    companion object {
        private val defaultImports = listOf(
            "kotlinx.coroutines.async",
            "dev.minn.jda.ktx.coroutines.await",
            "net.dv8tion.jda.api.managers.*",
            "net.dv8tion.jda.api.entities.*",
            "net.dv8tion.jda.api.*",
            "net.dv8tion.jda.api.utils.*",
            "net.dv8tion.jda.api.utils.data.*",
            "net.dv8tion.jda.internal.entities.*",
            "net.dv8tion.jda.internal.requests.*",
            "net.dv8tion.jda.api.requests.*",
            "java.io.*",
            "java.math.*",
            "java.util.*",
            "java.util.concurrent.*",
            "java.time.*"
        )

        private val timeoutScope = getDefaultScope(Executors.newSingleThreadScheduledExecutor())
        private val codeBlockRegex = Regex("""```(\w*)\s+(.+)```""")
        private val engine = ScriptEngineManager().getEngineByExtension("kts")!!

        private fun Throwable.stackTraceToDiscordString(): String {
            return unreflect().stackTraceToString()
                .lineSequence()
                .filterNot { "jdk.internal" in it }
                .filterNot { "kotlin.reflect.jvm.internal" in it }
                .filterNot { "kotlin.coroutines.jvm.internal" in it }
                .fold("") { acc, s ->
                    when {
                        acc.length + s.length <= Message.MAX_CONTENT_LENGTH - 256 -> acc + s + "\n"
                        else -> acc
                    }
                }.trimEnd()
        }
    }
}