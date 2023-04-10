package com.freya02.bot.commands.text

import com.freya02.bot.Config
import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.messageDeleteButton
import com.freya02.bot.utils.Emojis
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
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.FileProxy
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.concurrent.Executors
import java.util.function.Consumer
import javax.script.ScriptContext
import javax.script.ScriptEngineManager
import javax.script.SimpleBindings
import javax.script.SimpleScriptContext
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

// Some inspiration https://github.com/Xirado/Bean/blob/kotlin/src/main/kotlin/at/xirado/bean/command/legacy/EvalCommand.kt

private typealias MessageId = Long
private typealias OriginalMessageId = MessageId
private typealias ButtonId = String

@OptIn(ExperimentalTime::class)
@CommandMarker
class Eval(
    private val componentsService: Components,
    private val config: Config,
    private val coroutineScopes: BCoroutineScopesConfig,
    private val jda: JDA
) : TextCommand() {
    private class EvalState(val inputOffset: Int, val replyId: MessageId, val deleteButtonId: ButtonId, val context: ScriptContext, val timeoutJob: Job)

    private val states: MutableMap<OriginalMessageId, EvalState> = hashMapOf()

    override fun getDetailedDescription(): Consumer<EmbedBuilder> = Consumer {
        it.setAuthor("${jda.selfUser.name} - 'eval' command", null, jda.selfUser.effectiveAvatarUrl)

        it.appendDescription(
            """
                **Tip:** You can put your code in code blocks
                **Tip:** You can reply to an existing message to have it in a `message` variable
                **Tip:** FileProxy are shown as links and have download buttons
            """.trimIndent()
        )
    }

    @RequireOwner
    @JDATextCommand(name = "eval", description = "Evaluates Kotlin code")
    suspend fun onTextEval(event: BaseCommandEvent, @TextOption(name = "code", example = "guild.icon") input: String) {
        if (event.author.idLong !in config.ownerIds) {
            return event.reply("${event.author.asMention} is not in the sudoers file. This incident will be reported.").queue()
        }

        val bindings = mutableMapOf(
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

        event.message.messageReference?.let { messageReference ->
            bindings["message"] = messageReference.resolve().await()
        }

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
            measureTimedValue {
                withTimeout(1.minutes) {
                    (engine.eval(code, context) as Deferred<*>).await()
                }
            }
        }

        val deleteButton = componentsService.messageDeleteButton(message.author)
        val replyMessage = runResult.fold(
            onSuccess = { returnValue -> createMessage(returnValue, deleteButton) },
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

    private suspend fun createMessage(timedValue: TimedValue<Any?>, deleteButton: Button, addAttachments: Boolean = false): MessageCreateData {
        val (returnValue, duration) = timedValue
        return MessageCreate {
            val buttons: MutableList<Button> = arrayListOf()
            buttons += deleteButton

            embed {
                author(name = "Kotlin Eval - Evaluated in $duration", url = null, iconUrl = jda.selfUser.effectiveAvatarUrl)

                if (returnValue === Unit) {
                    description = "Code executed without errors"
                } else if (returnValue is FileProxy) {
                    field("Return value", "File at ${returnValue.url}")
                    if (addAttachments) {
                        val fileName = returnValue.url.toHttpUrl().pathSegments.last()
                        files += FileUpload.fromData(returnValue.download().await(), fileName)
                        image = "attachment://$fileName"
                    } else {
                        buttons += componentsService.ephemeralButton(ButtonStyle.SECONDARY, "Download attachment", Emojis.download) {
                            timeout(5.minutes)
                            bindTo { buttonEvent ->
                                buttonEvent.editComponents(buttonEvent.message.components.asDisabled()).queue()
                                val m = createMessage(timedValue, deleteButton, addAttachments = true)
                                buttonEvent.hook.editOriginal(MessageEditData.fromCreateData(m)).queue()
                            }
                        }
                    }
                } else {
                    field("Return value", "```\n${returnValue.toString()}```", false)
                }
            }

            components += buttons.row()
        }
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