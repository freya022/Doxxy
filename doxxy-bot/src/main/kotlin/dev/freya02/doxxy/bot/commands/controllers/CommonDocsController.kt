package dev.freya02.doxxy.bot.commands.controllers

import dev.freya02.doxxy.bot.commands.slash.DeleteButtonListener.Companion.messageDelete
import dev.freya02.doxxy.bot.commands.slash.docs.CommonDocsHandlers
import dev.freya02.doxxy.bot.config.BackendConfig
import dev.freya02.doxxy.bot.docs.DocResolveChain
import dev.freya02.doxxy.bot.docs.cached.CachedClass
import dev.freya02.doxxy.bot.docs.cached.CachedDoc
import dev.freya02.doxxy.bot.docs.cached.CachedMethod
import dev.freya02.doxxy.bot.docs.index.DocIndex
import dev.freya02.doxxy.bot.docs.index.DocSuggestion
import dev.freya02.doxxy.bot.examples.ExampleAPI
import dev.freya02.doxxy.bot.utils.joinLengthyString
import dev.freya02.doxxy.docs.DocSourceType
import dev.freya02.doxxy.docs.data.TargetType
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.InlineEmbed
import io.github.freya022.botcommands.api.components.Buttons
import io.github.freya022.botcommands.api.components.SelectMenus
import io.github.freya022.botcommands.api.components.builder.bindWith
import io.github.freya022.botcommands.api.components.data.InteractionConstraints
import io.github.freya022.botcommands.api.components.utils.ButtonContent
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.asUnicodeEmoji
import io.github.freya022.botcommands.api.pagination.Paginators
import io.github.freya022.botcommands.api.pagination.menu.buttonized.ButtonMenu
import io.github.freya022.botcommands.api.pagination.menu.buttonized.ButtonMenuBuilder
import io.github.freya022.botcommands.api.pagination.menu.buttonized.SuspendingChoiceCallback
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageCreateRequest
import net.fellbaum.jemoji.Emojis
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger { }

@BService
class CommonDocsController(
    private val backendConfig: BackendConfig,
    // null if backend is disabled
    private val exampleApi: ExampleAPI?,
    private val buttons: Buttons,
    private val selectMenus: SelectMenus,
    private val paginators: Paginators,
    private val classLinksController: ClassLinksController,
    private val methodLinksController: MethodLinksController
) {
    private object DocSuggestionButtonContentSupplier : ButtonMenu.ButtonContentSupplier<DocSuggestion> {
        override fun apply(item: DocSuggestion, index: Int): ButtonContent =
            ButtonContent.fromLabel(ButtonStyle.PRIMARY, "${index + 1}")
    }

    fun buildDocSuggestionsMenu(docIndex: DocIndex, suggestions: List<DocSuggestion>, user: UserSnowflake, callback: SuspendingChoiceCallback<DocSuggestion>, block: ButtonMenuBuilder<DocSuggestion>.() -> Unit) =
        paginators.buttonMenu(suggestions, DocSuggestionButtonContentSupplier, callback)
            .setConstraints(InteractionConstraints.ofUsers(user))
            .setTransformer { it.humanIdentifier }
            .setMaxEntriesPerPage(10)
            .setPageEditor { _, _, embedBuilder, _ ->
                InlineEmbed(embedBuilder).apply {
                    author {
                        name = when (docIndex.sourceType) {
                            DocSourceType.JAVA -> "Java Javadocs"
                            DocSourceType.JDA -> "JDA Javadocs"
                        }
                        iconUrl = when (docIndex.sourceType) {
                            DocSourceType.JAVA -> "https://assets.stickpng.com/images/58480979cef1014c0b5e4901.png"
                            DocSourceType.JDA -> "https://cdn.discordapp.com/icons/125227483518861312/8be466a3cdafc8591fcec4cdbb0eefc0.webp?size=128"
                            else -> null
                        }
                    }
                }
            }
            .apply(block)
            .build()

    suspend fun getDocMessageData(originalHook: InteractionHook?, caller: Member, ephemeral: Boolean, showCaller: Boolean, cachedDoc: CachedDoc, chain: DocResolveChain? = null): MessageCreateData {
        return MessageCreateBuilder().apply {
            addEmbeds(cachedDoc.embed.let {
                when {
                    showCaller || chain != null -> Embed {
                        builder.copyFrom(it)
                        if (showCaller)
                            author(caller.effectiveName, iconUrl = caller.effectiveAvatarUrl)
                        if (chain != null)
                            field("Resolved from", "`$chain`", true)

                        addUsableIn(cachedDoc)
                    }
                    else -> it
                }
            })
            addDocsSeeAlso(caller, cachedDoc)
            if (backendConfig.examples.fromDocs) {
                addExamples(cachedDoc)
            }
            addDocsActionRows(originalHook, ephemeral, cachedDoc, caller)
        }.build()
    }

    private suspend fun InlineEmbed.addUsableIn(cachedDoc: CachedDoc) {
        if (cachedDoc !is CachedMethod) return

        val className = cachedDoc.className
        val apiSubclasses = cachedDoc.docIndex.implementationIndex.getApiSubclasses(className)
        if (apiSubclasses.isNotEmpty())
            field("Usable in", apiSubclasses.joinLengthyString(
                separator = ", ",
                truncated = " and more...",
                lengthLimit = MessageEmbed.VALUE_MAX_LENGTH
            ) { subClass -> "[`${subClass.className}`](${subClass.sourceLink})" })
    }

    private suspend fun MessageCreateRequest<*>.addExamples(cachedDoc: CachedDoc) {
        if (exampleApi == null) return

        val examples = exampleApi.searchExamplesByTarget(cachedDoc.qualifiedName)
        if (examples.isEmpty()) return

        val selectMenu = selectMenus.stringSelectMenu().persistent {
            placeholder = "Examples"
            options += examples.map { SelectOption(it.title, it.title, emoji = Emojis.TEST_TUBE.asUnicodeEmoji()) }

            bindTo(CommonDocsHandlers.EXAMPLE_SELECT_LISTENER_NAME)
            timeout(14.days)
        }

        addActionRow(selectMenu)
    }

    private suspend fun MessageCreateRequest<*>.addDocsActionRows(
        originalHook: InteractionHook?,
        ephemeral: Boolean,
        cachedDoc: CachedDoc,
        caller: UserSnowflake
    ) {
        val list: List<ItemComponent> = buildList {
            cachedDoc.sourceLink?.let { sourceLink -> add(Button.link(sourceLink, "Source")) }

            if (cachedDoc is CachedClass)
                classLinksController.addCachedClassComponents(cachedDoc, originalHook, caller)
            else if (cachedDoc is CachedMethod)
                methodLinksController.addCachedMethodComponents(cachedDoc, originalHook, caller)

            if (!ephemeral) add(buttons.messageDelete(caller))
        }

        if (list.isNotEmpty()) {
            addActionRow(list)
        }
    }

    private suspend fun MessageCreateRequest<*>.addDocsSeeAlso(caller: UserSnowflake, cachedDoc: CachedDoc) {
        val docReferences = cachedDoc.seeAlsoReferences.filter { it.targetType != TargetType.UNKNOWN }
        if (docReferences.isEmpty()) return

        val selectMenu = selectMenus.stringSelectMenu().persistent {
            bindWith(CommonDocsHandlers::onSeeAlsoSelect, caller, cachedDoc.source)
            timeout(15.minutes)
            placeholder = "See also"

            for (reference in docReferences) {
                val optionValue = reference.targetType.name + ":" + reference.fullSignature
                if (optionValue.length > SelectMenu.ID_MAX_LENGTH) {
                    logger.warn { "Option value was too large (${optionValue.length}) for: '${optionValue}'" }
                    continue
                }

                addOption(reference.text, optionValue, Emojis.CLIPBOARD.asUnicodeEmoji())
            }
        }

        addActionRow(selectMenu)
    }
}
