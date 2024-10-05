package com.freya02.bot.examples

import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.messageDelete
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.into
import io.github.freya022.botcommands.api.components.Buttons
import io.github.freya022.botcommands.api.components.SelectMenus
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.toEditData
import io.github.freya022.botcommands.api.utils.EmojiUtils
import io.github.freya022.doxxy.common.dto.ExampleDTO.ExampleContentDTO.ExampleContentPartDTO
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import kotlin.time.Duration.Companion.minutes

@BService
class ExamplePaginatorFactory(private val buttons: Buttons, private val selectMenus: SelectMenus) {
    fun fromInteraction(
        parts: List<ExampleContentPartDTO>,
        author: UserSnowflake,
        ephemeral: Boolean,
        initialHook: InteractionHook
    ) = ExamplePaginator(
        buttons,
        selectMenus,
        parts,
        author,
        ephemeral,
        initialHook,
        onTimeout = { paginator, hook ->
            buttons.deleteRows(hook.retrieveOriginal().await().components)
            hook.editOriginal(paginator.createMessage(disabled = true).toEditData()).queue()
        }
    )
}

class ExamplePaginator(
    private val buttons: Buttons,
    private val selectMenus: SelectMenus,
    private val parts: List<ExampleContentPartDTO>,
    private val author: UserSnowflake,
    private val ephemeral: Boolean,
    // Keeping an up-to-date hook lets us clean up 15 minutes after each interaction
    private var hook: InteractionHook,
    private val onTimeout: suspend (ExamplePaginator, InteractionHook) -> Unit
) {
    private var page: Int = 0

    suspend fun createMessage(disabled: Boolean = false): MessageCreateData = MessageCreate {
        val currentPart = parts[page]

        content = currentPart.content
        if (parts.size > 1) {
            components += selectMenus.stringSelectMenu().ephemeral {
                parts.forEachIndexed { index, part ->
                    options += createSelectOption(part, index)
                }

                if (disabled) {
                    setDisabled(true)
                } else {
                    timeout(10.minutes) { onTimeout(this@ExamplePaginator, hook) }
                    bindTo { selectEvent ->
                        this@ExamplePaginator.hook = selectEvent.hook

                        val selectedIndex = selectEvent.values.single()
                        page = selectedIndex.toInt()

                        selectEvent.editMessage(createMessage().toEditData()).queue()
                        // Remember to clean up old components
                        selectMenus.deleteRows(selectEvent.message.components)
                    }
                }
            }.into()
        }
        if (!ephemeral) {
            components += buttons.messageDelete(author).into()
        }
    }

    private fun createSelectOption(part: ExampleContentPartDTO, index: Int) = SelectOption(
        part.label,
        index.toString(),
        part.description,
        part.emoji?.let {
            //TODO Resolving emojis should be done when updating examples, only storing their unicode/mention
            EmojiUtils.resolveJDAEmojiOrNull(it) ?: Emoji.fromFormatted(it)
        },
        default = page == index
    )
}