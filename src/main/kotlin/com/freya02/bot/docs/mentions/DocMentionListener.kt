package com.freya02.bot.docs.mentions

import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.core.annotations.BEventListener
import com.freya02.botcommands.api.utils.EmojiUtils
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.getDefaultScope
import dev.minn.jda.ktx.generics.getChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.Guild
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
    private val docMentionController: DocMentionController,
    private val docMentionRepository: DocMentionRepository
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
            docMentionController.createDocsMenuMessage(docMatches, event.userIdLong) {
                val channel = jda.getChannel<GuildMessageChannel>(channelId) ?: return@createDocsMenuMessage
                channel.deleteMessageById(messageId).queue(null, ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE))
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
}