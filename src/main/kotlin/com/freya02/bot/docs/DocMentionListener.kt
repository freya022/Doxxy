package com.freya02.bot.docs

import com.freya02.bot.db.Database
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.core.annotations.BEventListener
import com.freya02.botcommands.api.core.annotations.BService
import com.freya02.botcommands.api.utils.EmojiUtils
import com.freya02.docs.DocSourceType
import dev.minn.jda.ktx.events.getDefaultScope
import dev.minn.jda.ktx.generics.getChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.minutes

@BService //TODO remove
@CommandMarker
class DocMentionListener(private val database: Database) {
    private val logger = KotlinLogging.logger { }

    private val spaceRegex = Regex("""\s+""")
    private val codeBlockRegex = Regex("""```.*\n(\X*?)```""")
    private val identifierRegex = Regex("""(\w+)[#.](\w+)(?:\((.+?)\))?""")

    private val questionEmoji = EmojiUtils.resolveJDAEmoji("question")

    private val timeoutScope = getDefaultScope(pool = Executors.newSingleThreadScheduledExecutor() { Thread(it).also { t -> t.name = "DocMentionListener timeout thread" } })

    init {
        runBlocking {
            listOf(
                "Guild#auditlogs",
                "Guild",
                "Guild#retrieveAuditLogs",
                "Guild#retrieveAuditLog",
                "thread.sleep is never a solution",
                "use JDA#awaitReady",
                """Solved it

                ```java
                for(MessageEmbed emb : e.getMessage().getEmbeds()){
                    System.out.println(emb.getAuthor().getName());
                    System.out.println(emb.getTitle());
                    System.out.println(emb.getDescription());
                    for(MessageEmbed.Field field : emb.getFields()){
                        System.out.println(field.getName() + " : " + field.getValue());
                    }
                }
                System.out.println("------------------------------------");
                ```"""
            ).forEach {
                logger.debug(it)
                processMentions(it)
                println()
            }

//            exitProcess(0)
        }
    }

    //  Reaction added when a class/identifier is detected
    //      The reaction should be usable once per user
    //      The reaction expires after 5 minutes (removed, but can still be invoked)
    //
    //  Supported hints:
    //      Guild (exact casing)
    //      Guild#auditlogs (case-insensitive exact naming for the class, but fuzzy matching for identifier,
    //          select menu with 2 best distinct results including overloads)
    //      Guild#retrieveAuditLogs (case-insensitive exact naming for both, select menu for overloads
    //          is an extension of the rule above but for exact matches (similarity = 1))
    @BEventListener
    suspend fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.isWebhookMessage || event.author.isBot) return

        //Only analyse messages in help channel of the JDA guild
        if (event.guild.idLong == 125227483518861312) {
            if (!event.channelType.isThread) return
            if (event.channel.asThreadChannel().parentChannel.type != ChannelType.FORUM) return
        }

        val contentRaw = event.message.contentRaw
        val docMatches = processMentions(contentRaw)
        if (docMatches.isEmpty()) return

        event.message.addReaction(questionEmoji).queue {
            val jda = event.jda
            val channelId = event.message.channel.idLong
            val messageId = event.messageIdLong

            timeoutScope.launch {
                delay(5.minutes)

                val channel = jda.getChannel<GuildMessageChannel>(channelId) ?: return@launch
                channel.removeReactionById(messageId, questionEmoji).queue(null, ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE))
            }
        }
    }

    private suspend fun processMentions(contentRaw: String): DocMatches {
        val cleanedContent = codeBlockRegex.replace(contentRaw, "")

        val mentionedClasses = getMentionedClasses(cleanedContent)

        val similarIdentifiers: List<SimilarIdentifier> =
            identifierRegex.findAll(cleanedContent)
                .toList()
                .flatMap { result ->
                    database.preparedStatement(
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

        logger.debug { "Classes: $mentionedClasses" }
        logger.debug { "similarities = $similarIdentifiers" }

        return DocMatches(mentionedClasses, similarIdentifiers)
    }

    private suspend fun getMentionedClasses(content: String): List<ClassMention> {
        return spaceRegex.split(content).let {
            database.preparedStatement(
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

    private data class ClassMention(val sourceType: DocSourceType, val identifier: String)
    private data class SimilarIdentifier(val sourceType: DocSourceType, val identifier: String, val similarity: Float)
    private data class DocMatches(val classMentions: List<ClassMention>, val similarIdentifiers: List<SimilarIdentifier>) {
        val identicalIdentifiers: List<SimilarIdentifier>
            get() = similarIdentifiers.filter { it.similarity == 1.0f }

        fun isEmpty() = classMentions.isEmpty() && similarIdentifiers.isEmpty()
    }
}