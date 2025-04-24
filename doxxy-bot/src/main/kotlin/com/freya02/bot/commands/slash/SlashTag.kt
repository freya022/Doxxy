package com.freya02.bot.commands.slash

import com.freya02.bot.commands.filters.decl.NotJDACommandDeclarationFilter
import com.freya02.bot.tag.*
import com.freya02.bot.utils.isUniqueViolation
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.InlineEmbed
import dev.minn.jda.ktx.messages.reply_
import io.github.freya022.botcommands.api.commands.annotations.Command
import io.github.freya022.botcommands.api.commands.application.ApplicationCommand
import io.github.freya022.botcommands.api.commands.application.CommandScope
import io.github.freya022.botcommands.api.commands.application.annotations.DeclarationFilter
import io.github.freya022.botcommands.api.commands.application.slash.GuildSlashEvent
import io.github.freya022.botcommands.api.commands.application.slash.annotations.JDASlashCommand
import io.github.freya022.botcommands.api.commands.application.slash.annotations.SlashOption
import io.github.freya022.botcommands.api.commands.application.slash.annotations.TopLevelSlashCommandData
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.AutocompleteAlgorithms
import io.github.freya022.botcommands.api.commands.application.slash.autocomplete.annotations.AutocompleteHandler
import io.github.freya022.botcommands.api.components.Buttons
import io.github.freya022.botcommands.api.components.Components
import io.github.freya022.botcommands.api.components.data.InteractionConstraints
import io.github.freya022.botcommands.api.components.event.ButtonEvent
import io.github.freya022.botcommands.api.core.utils.deleteDelayed
import io.github.freya022.botcommands.api.core.utils.send
import io.github.freya022.botcommands.api.modals.*
import io.github.freya022.botcommands.api.modals.annotations.ModalData
import io.github.freya022.botcommands.api.modals.annotations.ModalHandler
import io.github.freya022.botcommands.api.modals.annotations.ModalInput
import io.github.freya022.botcommands.api.pagination.PageEditor
import io.github.freya022.botcommands.api.pagination.Paginators
import io.github.freya022.botcommands.api.pagination.paginator.Paginator
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.Permission.MANAGE_ROLES
import net.dv8tion.jda.api.Permission.MANAGE_SERVER
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import org.jetbrains.annotations.Contract
import java.sql.SQLException
import kotlin.time.Duration.Companion.minutes

private typealias TagConsumer = suspend (Tag) -> Unit

private const val GUILD_TAGS_AUTOCOMPLETE = "guildTagsAutocomplete"
private const val USER_TAGS_AUTOCOMPLETE = "userTagsAutocomplete"
private const val TAGS_CREATE_MODAL_HANDLER = "SlashTag: tagsCreate"
private const val TAGS_EDIT_MODAL_HANDLER = "SlashTag: tagsEdit"

@Command
class SlashTag(
    private val tagDB: TagDB,
    private val modals: Modals,
    private val buttons: Buttons,
    private val paginators: Paginators
) : ApplicationCommand() {
    @Contract(value = "null -> fail", pure = true)
    private fun <T> checkGuild(obj: T?): T = requireNotNull(obj) { "Event did not happen in a guild" }

    private suspend fun withOwnedTag(event: IReplyCallback, name: String, consumer: TagConsumer) {
        val guild = checkGuild(event.guild)
        val member = checkGuild(event.member)

        val tag = tagDB.get(guild.idLong, name) ?: run {
            event.reply("Tag '$name' was not found").setEphemeral(true).queue()
            return
        }

        if (tag.ownerId != event.user.idLong) {
            if (!member.hasPermission(event.guildChannel, MANAGE_SERVER, MANAGE_ROLES)) {
                event.reply("You do not own this tag").setEphemeral(true).queue()
                return
            }
        }

        consumer(tag)
    }

    private suspend fun withTag(event: GuildSlashEvent, name: String, consumer: TagConsumer) {
        val tag = tagDB.get(event.guild.idLong, name) ?: run {
            event.reply("Tag '$name' was not found").setEphemeral(true).queue()
            return
        }

        consumer(tag)
    }

    @TopLevelSlashCommandData(scope = CommandScope.GUILD)
    @DeclarationFilter(NotJDACommandDeclarationFilter::class)
    @JDASlashCommand(name = "tag", description = "Sends a predefined tag")
    suspend fun sendTag(
        event: GuildSlashEvent,
        @SlashOption(description = "Name of the tag", autocomplete = GUILD_TAGS_AUTOCOMPLETE) name: String
    ) {
        withTag(event, name) { tag: Tag ->
            tagDB.incrementTag(event.guild.idLong, tag.name)
            event.reply(tag.content).queue()
        }
    }

    @TopLevelSlashCommandData(scope = CommandScope.GUILD, description = "Manage tags")
    @DeclarationFilter(NotJDACommandDeclarationFilter::class)
    @JDASlashCommand(
        name = "tags",
        subcommand = "raw",
        description = "Sends a predefined tag with all the markdown escaped"
    )
    suspend fun sendRawTag(
        event: GuildSlashEvent,
        @SlashOption(description = "Name of the tag", autocomplete = GUILD_TAGS_AUTOCOMPLETE) name: String
    ) {
        withTag(event, name) { tag: Tag -> event.reply(MarkdownSanitizer.escape(tag.content)).queue() }
    }

    @DeclarationFilter(NotJDACommandDeclarationFilter::class)
    @JDASlashCommand(name = "tags", subcommand = "create", description = "Creates a tag in this guild")
    fun createTag(event: GuildSlashEvent) {
        val modal = modals.create("Create a tag") {
            shortTextInput("tagName", "Tag name") {
                minLength = TagDB.NAME_MIN_LENGTH
                maxLength = TagDB.NAME_MAX_LENGTH
            }

            shortTextInput("tagDescription", "Tag description") {
                minLength = TagDB.DESCRIPTION_MIN_LENGTH
                maxLength = TagDB.DESCRIPTION_MAX_LENGTH
            }

            paragraphTextInput("tagContent", "Tag content") {
                minLength = TagDB.CONTENT_MIN_LENGTH
                maxLength = TagDB.CONTENT_MAX_LENGTH
            }

            bindTo(TAGS_CREATE_MODAL_HANDLER)
        }

        event.replyModal(modal).queue()
    }

    @ModalHandler(name = TAGS_CREATE_MODAL_HANDLER)
    suspend fun createTag(
        event: ModalEvent,
        @ModalInput(name = "tagName") name: String,
        @ModalInput(name = "tagDescription") description: String,
        @ModalInput(name = "tagContent") content: String
    ) {
        try {
            tagDB.create(checkGuild(event.guild).idLong, event.user.idLong, name, description, content)
            event.replyFormat("Tag '%s' created successfully", name).setEphemeral(true).queue()
        } catch (e: SQLException) {
            if (e.isUniqueViolation()) {
                event.replyFormat("Tag '%s' already exists", name).setEphemeral(true).queue()
            } else {
                throw e
            }
        } catch (e: TagException) {
            event.reply(e.message.toString()).setEphemeral(true).queue()
        }
    }

    @DeclarationFilter(NotJDACommandDeclarationFilter::class)
    @JDASlashCommand(name = "tags", subcommand = "edit", description = "Edits a tag in this guild")
    suspend fun editTag(
        event: GuildSlashEvent,
        @SlashOption(description = "Name of the tag", autocomplete = USER_TAGS_AUTOCOMPLETE) name: String
    ) {
        withOwnedTag(event, name) { tag: Tag ->
            val modal = modals.create("Edit a tag") {
                shortTextInput("tagName", "Tag name") {
                    value = tag.name
                }

                shortTextInput("tagDescription", "Tag description") {
                    value = tag.description
                }

                paragraphTextInput("tagContent", "Tag content") {
                    value = tag.content
                }

                bindTo(TAGS_EDIT_MODAL_HANDLER, name)
            }

            event.replyModal(modal).queue()
        }
    }

    @ModalHandler(name = TAGS_EDIT_MODAL_HANDLER)
    suspend fun editTag(
        event: ModalEvent,
        @ModalData name: String,
        @ModalInput(name = "tagName") newName: String,
        @ModalInput(name = "tagDescription") newDescription: String,
        @ModalInput(name = "tagContent") newContent: String
    ) {
        withOwnedTag(event, name) {
            try {
                tagDB.edit(checkGuild(event.guild).idLong, event.user.idLong, name, newName, newDescription, newContent)
                event.replyFormat("Tag '%s' edited successfully", name).setEphemeral(true).queue()
            } catch (e: TagException) {
                event.reply(e.message.toString()).setEphemeral(true).queue()
            }
        }
    }

    @DeclarationFilter(NotJDACommandDeclarationFilter::class)
    @JDASlashCommand(
        name = "tags",
        subcommand = "transfer",
        description = "Transfers a tag ownership to someone else in this guild"
    )
    suspend fun transferTag(
        event: GuildSlashEvent,
        @SlashOption(description = "Name of the tag", autocomplete = USER_TAGS_AUTOCOMPLETE) name: String,
        @SlashOption(description = "Member to transfer the tag to") newOwner: Member
    ) {
        if (newOwner.user.isBot) {
            event.reply("The member to transfer the tag to cannot be a bot").setEphemeral(true).queue()
            return
        }
        withOwnedTag(event, name) {
            tagDB.transfer(
                event.guild.idLong,
                event.user.idLong,
                name,
                newOwner.idLong
            )

            event.replyFormat("Tag '%s' transfer to %s successfully", name, newOwner.asMention)
                .setEphemeral(true)
                .queue()
        }
    }

    @DeclarationFilter(NotJDACommandDeclarationFilter::class)
    @JDASlashCommand(name = "tags", subcommand = "delete", description = "Deletes a tag you own in this guild")
    suspend fun deleteTag(
        event: GuildSlashEvent,
        @SlashOption(description = "Name of the tag", autocomplete = USER_TAGS_AUTOCOMPLETE) name: String
    ) {
        withOwnedTag(event, name) { tag: Tag ->
            val deleteButton = buttons.danger("Delete").ephemeral {
                singleUse = true
                noTimeout()
                bindTo { doDeleteTag(event, name, it) }
            }
            val noButton = buttons.primary("No").ephemeral {
                singleUse = true
                noTimeout()
                bindTo { it.editMessage("Cancelled").setComponents().queue() }
            }
            buttons.group(deleteButton, noButton).ephemeral {
                // Default timeout
            }

            event.reply_("Are you sure you want to delete the tag '${tag.name}'?", ephemeral = true)
                .addActionRow(deleteButton, noButton)
                .deleteDelayed(event.hook, Components.defaultEphemeralTimeout)
                .queue()
        }
    }

    private suspend fun doDeleteTag(event: GuildSlashEvent, name: String, btnEvt: ButtonEvent) {
        tagDB.delete(event.guild.idLong, event.user.idLong, name)
        btnEvt.editMessageFormat("Tag '%s' deleted successfully", name).setComponents().queue()
    }

    @DeclarationFilter(NotJDACommandDeclarationFilter::class)
    @JDASlashCommand(name = "tags", subcommand = "list", description = "Creates a tag in this guild")
    suspend fun listTags(
        event: GuildSlashEvent,
        @SlashOption(name = "sorting", description = "Type of tag sorting", usePredefinedChoices = true) criteria: TagCriteria = TagCriteria.NAME
    ) {
        val totalTags = tagDB.getTotalTags(event.guild.idLong)
        val pageEditor = PageEditor<Paginator> { _, _, embedBuilder, page: Int ->
            InlineEmbed(embedBuilder).apply {
                try {
                    val tagRange = tagDB.getTagRange(event.guild.idLong, criteria, 10 * page, 20)

                    title = "All tags for " + event.guild.name

                    description = when {
                        tagRange.isEmpty() -> "No tags for this guild"
                        else -> tagRange.joinToString("\n") { t: Tag -> "${t.name} : ${t.description} : <@${t.ownerId}> (${t.uses} uses)" }
                    }
                } catch (e: SQLException) {
                    logger.error(e) { "An exception occurred while paginating through tags in guild '${event.guild.name}' (${event.guild.id})" }
                    title = "Unable to get the tags"
                }
            }
        }

        paginators.paginator(totalTags / 10, pageEditor)
            .setConstraints(InteractionConstraints.ofUsers(event.user))
            .setTimeout(5.minutes) { p: Paginator ->
                p.cleanup()
                event.hook.editOriginalComponents().queue()
            }
            .build()
            .getInitialMessage()
            .send(event, ephemeral = true)
            .queue()
    }

    @DeclarationFilter(NotJDACommandDeclarationFilter::class)
    @JDASlashCommand(name = "tags", subcommand = "info", description = "Gives information about a tag in this guild")
    suspend fun infoTags(
        event: GuildSlashEvent,
        @SlashOption(description = "Name of the tag", autocomplete = GUILD_TAGS_AUTOCOMPLETE) tagName: String
    ) {
        withTag(event, tagName) { tag: Tag ->
            event.deferReply(true).queue() //Retrieve might take some time, ig

            val embed = Embed {
                val ownerMember = event.guild.retrieveMemberById(tag.ownerId).await()
                if (ownerMember != null) {
                    author {
                        name = ownerMember.user.name
                        iconUrl = ownerMember.effectiveAvatarUrl
                    }
                } else {
                    val owner = event.jda.retrieveUserById(tag.ownerId).await()

                    author {
                        name = owner.name
                        iconUrl = owner.effectiveAvatarUrl
                    }
                }

                title = "Tag '${tag.name}'"

                field("Description", tag.description, false)
                field("Owner", "<@" + tag.ownerId + ">", true)
                field("Uses", tag.uses.toString(), true)
                field("Rank", tagDB.getRank(event.guild.idLong, tagName).toString(), true)

                footer {
                    name = "Created on"
                    timestamp = tag.createdAt
                }
            }

            event.hook.sendMessageEmbeds(embed).setEphemeral(true).queue()
        }
    }

    @AutocompleteHandler(name = GUILD_TAGS_AUTOCOMPLETE, showUserInput = false)
    fun guildTagsAutocomplete(event: CommandAutoCompleteInteractionEvent): List<Choice> {
        val guild = checkGuild(event.guild)
        return AutocompleteAlgorithms
            .fuzzyMatching(
                tagDB.getShortTagsSorted(guild.idLong, TagCriteria.USES), { obj: ShortTag -> obj.name },
                event.focusedOption.value
            )
            .map { r -> Choice(r.item.asChoiceName(), r.string) }
    }

    @AutocompleteHandler(name = USER_TAGS_AUTOCOMPLETE, showUserInput = false)
    fun userTagsAutocomplete(event: CommandAutoCompleteInteractionEvent): List<Choice> {
        val guild = checkGuild(event.guild)
        return AutocompleteAlgorithms
            .fuzzyMatching(
                tagDB.getShortTagsSorted(guild.idLong, event.user.idLong, TagCriteria.NAME),
                { obj: ShortTag -> obj.name },
                event.focusedOption.value
            )
            .map { r -> Choice(r.item.asChoiceName(), r.string) }
    }

    private fun ShortTag.asChoiceName(): String {
        val choiceName = "$name - $description"
        return when {
            choiceName.length > OptionData.MAX_CHOICE_NAME_LENGTH -> name
            //TODO maybe improve
            else -> choiceName
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}
