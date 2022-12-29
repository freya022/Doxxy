package com.freya02.bot.docs.mentions

import com.freya02.bot.commands.controllers.CommonDocsController
import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.messageDeleteButton
import com.freya02.bot.docs.DocIndexMap
import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.components.builder.select.ephemeral.EphemeralStringSelectBuilder
import com.freya02.botcommands.api.components.event.StringSelectEvent
import com.freya02.botcommands.api.core.db.Database
import com.freya02.botcommands.api.core.db.KConnection
import com.freya02.docs.DocSourceType
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.entities.UserSnowflake
import java.util.*
import kotlin.time.Duration.Companion.minutes

class DocMentionController(
    private val database: Database,
    private val componentsService: Components,
    private val docIndexMap: DocIndexMap,
    private val commonDocsController: CommonDocsController
) {
    private val spaceRegex = Regex("""\s+""")
    private val codeBlockRegex = Regex("""```.*\n(\X*?)```""")
    private val identifierRegex = Regex("""(\w+)[#.](\w+)(?:\((.+?)\))?""")

    suspend fun processMentions(contentRaw: String): DocMatches = database.withConnection(readOnly = true) {
        val cleanedContent = codeBlockRegex.replace(contentRaw, "")

        val mentionedClasses = getMentionedClasses(cleanedContent)

        val similarIdentifiers: SortedSet<SimilarIdentifier> =
            identifierRegex.findAll(cleanedContent)
                .flatMapTo(sortedSetOf()) { result ->
                    preparedStatement(
                        """
                            select d.source_id,
                                   d.classname || '#' || d.identifier as fullIdentifier,
                                   d.human_class_identifier,
                                   similarity(d.classname, ?) * similarity(d.identifier_no_args, ?) as overall_similarity
                            from doc d
                            where d.type != 1
                            order by overall_similarity desc
                            limit 25;
                        """.trimIndent()
                    ) {
                        executeQuery(result.groupValues[1], result.groupValues[2])
                            .mapNotNull {
                                val sourceId: Int = it["source_id"]

                                //Can't use that in a select menu :/
                                if (it.getString("fullIdentifier").length >= 95) {
                                    return@mapNotNull null
                                }

                                SimilarIdentifier(
                                    DocSourceType.fromId(sourceId),
                                    it["fullIdentifier"],
                                    it["human_class_identifier"],
                                    it["overall_similarity"]
                                )
                            }
                            .let { similarIdentifiers -> // If the token has been fully matched then only keep it and it's overloads
                                when (similarIdentifiers.first().similarity) {
                                    1.0f -> similarIdentifiers.filter { it.similarity == 1.0f }
                                    else -> similarIdentifiers
                                }
                            }
                    }
                }

        return DocMatches(mentionedClasses, similarIdentifiers)
    }

    suspend fun createDocsMenuMessage(
        docMatches: DocMatches,
        callerId: Long,
        timeoutCallback: suspend () -> Unit
    ) = MessageCreate {
        val docsMenu = componentsService.ephemeralStringSelectMenu {
            addMatchOptions(docMatches)

            timeout(5.minutes, timeoutCallback)

            bindTo { selectEvent -> onSelectedDoc(selectEvent) }
        }

        val deleteButton = componentsService.messageDeleteButton(UserSnowflake.fromId(callerId))
        components += row(docsMenu)
        components += row(deleteButton)
    }

    context(KConnection)
    private suspend fun getMentionedClasses(content: String): List<ClassMention> {
        return spaceRegex.split(content).let {
            preparedStatement(
                """
                    select d.source_id, d.classname
                    from doc d
                    where d.type = 1
                      and classname = any (?);
                """.trimIndent()
            ) {
                executeQuery(it.toTypedArray()).map {
                    val sourceId: Int = it["source_id"]
                    ClassMention(
                        DocSourceType.fromId(sourceId),
                        it["classname"]
                    )
                }
            }
        }
    }

    private fun EphemeralStringSelectBuilder.addMatchOptions(docMatches: DocMatches) {
        docMatches.classMentions.forEach {
            addOption(it.identifier, "${it.sourceType.id}:${it.identifier}")
        }

        docMatches.similarIdentifiers.forEach {
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

        selectEvent.reply(commonDocsController.getDocMessageData(selectEvent.member!!,
            ephemeral = false,
            showCaller = true,
            cachedDoc = doc
        )).setEphemeral(false).queue()
    }
}