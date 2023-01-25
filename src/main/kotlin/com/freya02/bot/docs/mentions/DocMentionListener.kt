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
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.exceptions.ErrorHandler
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.utils.TimeUtil
import java.util.concurrent.Executors
import kotlin.properties.Delegates
import kotlin.time.Duration.Companion.hours
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
    //      The reaction expires after 5 minutes (removed, but can still be invoked **only if the message is recent enough**, see conditions below)
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

    //  A select menu can be created once per message per user
    //  Can be created if relative position < 12 and created < 2 hours
    //  That select menu can be used by the caller and will auto delete after 1 minute
    //  Will have to put a small embed to know who used the feature as discord does not show who made the select menu
    @BEventListener
    suspend fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        if (!event.isFromGuild) return
        if (event.userIdLong == event.jda.selfUser.idLong) return
        if (event.emoji != questionEmoji) return
        if (!checkChannel(event.guild, event.channel)) return

        val reactedMessageId = event.messageIdLong
        val twoHoursAgoSnowflake = TimeUtil.getDiscordTimestamp(System.currentTimeMillis() - 2.hours.inWholeMilliseconds)
        if (reactedMessageId < twoHoursAgoSnowflake) return //Message is too old

        docMentionRepository.ifNotUsed(event.messageIdLong, event.userIdLong) {
            val message = event.retrieveMessage().await()

            //Check difference of at most 12 messages
            (event.channel as? ThreadChannel)?.let { threadChannel ->
                if (threadChannel.totalMessageCount - message.approximatePosition > 12) return@ifNotUsed
            }

            val docMatches = docMentionController.processMentions(message.contentRaw)
            if (!docMatches.isSufficient()) return@ifNotUsed

            //Setting the message ID after sending it definitely hurts
            val jda = event.jda
            val channelId = event.channel.idLong
            var messageId: Long by Delegates.notNull()
            docMentionController.createDocsMenuMessage(
                docMatches,
                UserSnowflake.fromId(event.userIdLong),
                timeoutCallback = {
                    val channel = jda.getChannel<GuildMessageChannel>(channelId) ?: return@createDocsMenuMessage
                    channel.deleteMessageById(messageId).queue(null, ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE))
                }
            ).also {
                messageId = event.channel.sendMessage(it)
                    .setMessageReference(event.messageIdLong)
                    .mentionRepliedUser(false)
                    .await()
                    .idLong
            }
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
