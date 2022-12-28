package com.freya02.bot.docs.mentions

import com.freya02.botcommands.api.core.db.Database
import com.freya02.botcommands.api.core.db.KConnection
import com.freya02.docs.DocSourceType
import java.util.*

class DocMentionController(private val database: Database) {
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
                                   d.human_class_identifier,
                                   similarity(d.classname, ?) * similarity(d.identifier_no_args, ?) as overall_similarity
                            from doc d
                            where d.type != 1
                            order by overall_similarity desc
                            limit 25;
                        """.trimIndent()
                    ) {
                        executeQuery(result.groupValues[1], result.groupValues[2])
                            .map {
                                val sourceId: Int = it["source_id"]

                                SimilarIdentifier(
                                    DocSourceType.fromId(sourceId),
                                    it["human_class_identifier"],
                                    it["overall_similarity"]
                                )
                            }
                    }
                }

        return DocMatches(mentionedClasses, similarIdentifiers)
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
}