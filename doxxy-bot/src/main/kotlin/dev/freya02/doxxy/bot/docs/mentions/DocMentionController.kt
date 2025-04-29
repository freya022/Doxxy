package dev.freya02.doxxy.bot.docs.mentions

import dev.freya02.doxxy.bot.commands.controllers.CommonDocsController
import dev.freya02.doxxy.bot.commands.slash.DeleteButtonListener.Companion.messageDelete
import dev.freya02.doxxy.bot.docs.DocIndexMap
import dev.freya02.doxxy.bot.utils.ParsingUtils.codeBlockRegex
import dev.freya02.doxxy.bot.utils.ParsingUtils.spaceRegex
import dev.freya02.doxxy.docs.DocSourceType
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.botcommands.api.components.Buttons
import io.github.freya022.botcommands.api.components.SelectMenus
import io.github.freya022.botcommands.api.components.builder.select.ephemeral.EphemeralStringSelectBuilder
import io.github.freya022.botcommands.api.components.event.StringSelectEvent
import io.github.freya022.botcommands.api.core.db.Database
import io.github.freya022.botcommands.api.core.db.Transaction
import io.github.freya022.botcommands.api.core.db.transactional
import io.github.freya022.botcommands.api.core.service.annotations.BService
import io.github.freya022.botcommands.api.core.utils.edit
import io.github.freya022.botcommands.api.core.utils.toEditData
import net.dv8tion.jda.api.entities.Message.MentionType
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import java.util.*
import kotlin.time.Duration.Companion.minutes

@BService
class DocMentionController(
    private val database: Database,
    private val buttons: Buttons,
    private val selectMenus: SelectMenus,
    private val docIndexMap: DocIndexMap,
    private val commonDocsController: CommonDocsController
) {
    private val identifierRegex = Regex("""(\w+)[#.](\w+)""")

    suspend fun processMentions(contentRaw: String): DocMatches = database.transactional(readOnly = true) {
        val cleanedContent = codeBlockRegex.replace(contentRaw, "")

        val mentionedClasses = getMentionedClasses(cleanedContent)

        val similarIdentifiers: SortedSet<SimilarIdentifier> =
            identifierRegex.findAll(cleanedContent)
                .map { it.destructured }
                .flatMapTo(sortedSetOf()) { (classname, identifierNoArgs) ->
                    preparedStatement(
                        """
                            select source_id,
                                   full_identifier,
                                   human_class_identifier,
                                   similarity(classname, ?) * similarity(identifier_no_args, ?) as overall_similarity
                            from doc_view
                                     natural join doc
                            where type != 1
                              and classname % ?
                              and identifier_no_args % ?
                            order by overall_similarity desc
                            limit 25;
                        """.trimIndent()
                    ) {
                        executeQuery(classname, identifierNoArgs, classname, identifierNoArgs)
                            .mapNotNull {
                                val sourceId = it.getInt("source_id")

                                //Can't use that in a select menu :/
                                if (it.getString("full_identifier").length >= 95) {
                                    return@mapNotNull null
                                }

                                SimilarIdentifier(
                                    DocSourceType.fromId(sourceId),
                                    it["full_identifier"],
                                    it["human_class_identifier"],
                                    it["overall_similarity"]
                                )
                            }
                            .let { similarIdentifiers -> // If the token has been fully matched then only keep it and it's overloads
                                when (similarIdentifiers.firstOrNull()?.similarity) {
                                    1.0f -> similarIdentifiers.filter { it.similarity == 1.0f }
                                    else -> similarIdentifiers
                                }
                            }
                    }
                }

        DocMatches(mentionedClasses, similarIdentifiers)
    }

    suspend fun createDocsMenuMessage(
        docMatches: DocMatches,
        caller: UserSnowflake,
        timeoutCallback: suspend () -> Unit
    ): MessageCreateData {
        return MessageCreate {
            val docsMenu = selectMenus.stringSelectMenu().ephemeral {
                placeholder = "Select a doc"
                addMatchOptions(docMatches)

                constraints += caller
                timeout(1.minutes, timeoutCallback)
                bindTo { selectEvent -> onSelectedDoc(selectEvent) }
            }

            val deleteButton = buttons.messageDelete(caller)

            components += row(docsMenu)
            components += row(deleteButton)

            content = "${caller.asMention} This message will be deleted in a minute"
            allowedMentionTypes = EnumSet.noneOf(MentionType::class.java) //No mentions
        }
    }

    context(Transaction)
    private suspend fun getMentionedClasses(content: String): List<ClassMention> {
        return spaceRegex.split(content).let { words ->
            preparedStatement(
                """
                    select d.source_id, d.classname
                    from doc d
                    where d.type = 1
                      and classname = any (?);
                """.trimIndent()
            ) {
                executeQuery(words.toTypedArray()).map {
                    val sourceId = it.getInt("source_id")
                    ClassMention(
                        DocSourceType.fromId(sourceId),
                        it["classname"]
                    )
                }
            }
        }
    }

    private fun EphemeralStringSelectBuilder.addMatchOptions(docMatches: DocMatches) {
        docMatches.classMentions.take(SelectMenu.OPTIONS_MAX_AMOUNT).forEach {
            addOption(it.identifier, "${it.sourceType.id}:${it.identifier}")
        }

        docMatches.similarIdentifiers
            .take(SelectMenu.OPTIONS_MAX_AMOUNT - options.size)
            .forEach {
                addOption(it.fullHumanIdentifier, "${it.sourceType.id}:${it.fullIdentifier}")
            }
    }

    private suspend fun onSelectedDoc(selectEvent: StringSelectEvent) {
        val (sourceTypeId, identifier) = selectEvent.values.single().split(':')

        val doc = sourceTypeId.toIntOrNull()
            ?.let { DocSourceType.fromIdOrNull(it) }
            ?.let { docIndexMap[it] }
            ?.let { docIndex ->
                when {
                    '(' in identifier -> docIndex.getMethodDoc(identifier)
                    '#' in identifier -> docIndex.getFieldDoc(identifier)
                    else -> docIndex.getClassDoc(identifier)
                }
            }

        if (doc == null) {
            selectEvent.reply_("This doc is not available anymore", ephemeral = true).queue()
            return
        }

        commonDocsController.getDocMessageData(
            selectEvent.hook,
            selectEvent.member!!,
            ephemeral = false,
            showCaller = false,
            cachedDoc = doc
        ).toEditData()
            .edit(selectEvent)
            .setReplace(true)
            .queue()
            .also {
                //Delete the components of the current message,
                // we don't want the timeout to execute
                buttons.deleteRows(selectEvent.message.components)
            }
    }
}
