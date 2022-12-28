package com.freya02.bot.docs.mentions

import com.freya02.bot.commands.controllers.CommonDocsController
import com.freya02.bot.commands.slash.DeleteButtonListener.Companion.messageDeleteButton
import com.freya02.bot.docs.DocIndexMap
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.components.builder.select.ephemeral.EphemeralStringSelectBuilder
import com.freya02.botcommands.api.components.event.StringSelectEvent
import com.freya02.botcommands.api.core.annotations.BEventListener
import com.freya02.botcommands.api.utils.EmojiUtils
import com.freya02.docs.DocSourceType
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.getDefaultScope
import dev.minn.jda.ktx.generics.getChannel
import dev.minn.jda.ktx.interactions.components.asDisabled
import dev.minn.jda.ktx.interactions.components.row
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.reply_
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import java.util.concurrent.Executors
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.minutes

@CommandMarker
class DocMentionListener(
    private val componentsService: Components,
    private val docMentionController: DocMentionController,
    private val docMentionRepository: DocMentionRepository,
    private val docIndexMap: DocIndexMap,
    private val commonDocsController: CommonDocsController
) {
    private val questionEmoji = EmojiUtils.resolveJDAEmoji("question")

    private val timeoutScope = getDefaultScope(pool = Executors.newSingleThreadScheduledExecutor { Thread(it).also { t -> t.name = "DocMentionListener timeout thread" } })

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
        if (!event.isFromGuild) return
        if (event.isWebhookMessage || event.author.isBot) return

        //Only analyse messages in help channel of the JDA guild
        if (!checkChannel(event.guild, event.channel)) return

        val contentRaw = event.message.contentRaw
        val docMatches = docMentionController.processMentions(contentRaw)
        if (!docMatches.isSufficient()) return

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

    @BEventListener
    suspend fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        if (!event.isFromGuild) return
        if (event.userIdLong == event.jda.selfUser.idLong) return
        if (event.emoji != questionEmoji) return
        if (!checkChannel(event.guild, event.channel)) return

        docMentionRepository.ifNotUsed(event.messageIdLong, event.userIdLong) {
            val message = event.retrieveMessage().await()

            val docMatches = docMentionController.processMentions(message.contentRaw)
            if (!docMatches.isSufficient()) return@ifNotUsed

            //Setting the message ID after sending it definitely hurts
            val jda = event.jda
            val channelId = event.channel.idLong
            var messageId: Long by Delegates.notNull()
            MessageCreate {
                val docsMenu = componentsService.ephemeralStringSelectMenu {
                    addMatchOptions(docMatches)

                    timeout(5.minutes) {
                        val channel = jda.getChannel<GuildMessageChannel>(channelId) ?: return@timeout
                        channel.retrieveMessageById(messageId)
                            .flatMap { message.editMessageComponents(message.components.asDisabled()) }
                            .queue()
                    }

                    bindTo { selectEvent -> onSelectedDoc(selectEvent) }
                }

                val deleteButton = componentsService.messageDeleteButton(UserSnowflake.fromId(event.userIdLong))
                components += row(docsMenu)
                components += row(deleteButton)
            }.let { messageId = event.channel.sendMessage(it).await().idLong }
        }
    }

    private fun checkChannel(guild: Guild, channel: MessageChannelUnion): Boolean {
        if (guild.idLong == 125227483518861312) {
            if (!channel.type.isThread) return false
            if (channel.asThreadChannel().parentChannel.type != ChannelType.FORUM) return false
        }

        return true
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

        selectEvent.reply(commonDocsController.getDocMessageData(selectEvent.member!!, true, doc))
            .setEphemeral(true)
            .queue()
    }
}