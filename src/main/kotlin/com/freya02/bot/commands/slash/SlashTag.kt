package com.freya02.bot.commands.slash

import com.freya02.bot.db.Database
import com.freya02.bot.db.isUniqueViolation
import com.freya02.bot.tag.*
import com.freya02.botcommands.api.Logging
import com.freya02.botcommands.api.annotations.CommandMarker
import com.freya02.botcommands.api.application.ApplicationCommand
import com.freya02.botcommands.api.application.CommandScope
import com.freya02.botcommands.api.application.annotations.AppOption
import com.freya02.botcommands.api.application.slash.GuildSlashEvent
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand
import com.freya02.botcommands.api.application.slash.autocomplete.AutocompleteAlgorithms
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.AutocompletionHandler
import com.freya02.botcommands.api.components.Components
import com.freya02.botcommands.api.components.InteractionConstraints
import com.freya02.botcommands.api.components.event.ButtonEvent
import com.freya02.botcommands.api.modals.Modals
import com.freya02.botcommands.api.modals.annotations.ModalData
import com.freya02.botcommands.api.modals.annotations.ModalHandler
import com.freya02.botcommands.api.modals.annotations.ModalInput
import com.freya02.botcommands.api.pagination.paginator.Paginator
import com.freya02.botcommands.api.pagination.paginator.PaginatorBuilder
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import me.xdrop.fuzzywuzzy.model.BoundExtractedResult
import net.dv8tion.jda.api.Permission.MANAGE_ROLES
import net.dv8tion.jda.api.Permission.MANAGE_SERVER
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import org.jetbrains.annotations.Contract
import java.sql.SQLException
import java.util.concurrent.TimeUnit


private typealias TagConsumer = suspend (Tag) -> Unit

private val LOGGER = Logging.getLogger()

private const val GUILD_TAGS_AUTOCOMPLETE = "guildTagsAutocomplete"
private const val USER_TAGS_AUTOCOMPLETE = "userTagsAutocomplete"
private const val TAGS_CREATE_MODAL_HANDLER = "SlashTag: tagsCreate"
private const val TAGS_EDIT_MODAL_HANDLER = "SlashTag: tagsEdit"

@CommandMarker
class SlashTag(database: Database) : ApplicationCommand() {
    private val tagDB: TagDB = TagDB(database)

    @Contract(value = "null -> fail", pure = true)
    private fun <T> checkGuild(obj: T?): T = requireNotNull(obj) { "Event did not happen in a guild" }

    private suspend fun withOwnedTag(event: IReplyCallback, name: String, consumer: TagConsumer) {
        val guild = checkGuild(event.guild)
        val member = checkGuild(event.member)

        val tag = tagDB[guild.idLong, name] ?: run {
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
        val tag = tagDB[event.guild.idLong, name] ?: run {
            event.reply("Tag '$name' was not found").setEphemeral(true).queue()
            return
        }

        consumer(tag)
    }

    @JDASlashCommand(scope = CommandScope.GLOBAL_NO_DM, name = "tag", description = "Sends a predefined tag")
    suspend fun sendTag(
        event: GuildSlashEvent,
        @AppOption(description = "Name of the tag", autocomplete = GUILD_TAGS_AUTOCOMPLETE) name: String
    ) {
        withTag(event, name) { tag: Tag ->
            tagDB.incrementTag(event.guild.idLong, tag.name)
            event.reply(tag.content).queue()
        }
    }

    @JDASlashCommand(
        scope = CommandScope.GLOBAL_NO_DM,
        name = "tags",
        subcommand = "raw",
        description = "Sends a predefined tag with all the markdown escaped"
    )
    suspend fun sendRawTag(
        event: GuildSlashEvent,
        @AppOption(description = "Name of the tag", autocomplete = GUILD_TAGS_AUTOCOMPLETE) name: String
    ) {
        withTag(event, name) { tag: Tag -> event.reply(MarkdownSanitizer.escape(tag.content)).queue() }
    }

    @JDASlashCommand(scope = CommandScope.GLOBAL_NO_DM, name = "tags", subcommand = "create", description = "Creates a tag in this guild")
    fun createTag(event: GuildSlashEvent) {
        val modal = Modals.create("Create a tag", TAGS_CREATE_MODAL_HANDLER)
            .addActionRow(Modals.createTextInput("tagName", "Tag name", TextInputStyle.SHORT).build())
            .addActionRow(Modals.createTextInput("tagDescription", "Tag description", TextInputStyle.SHORT).build())
            .addActionRow(Modals.createTextInput("tagContent", "Tag content", TextInputStyle.PARAGRAPH).build())
            .build()

        event.replyModal(modal).queue()
    }

    @ModalHandler(name = TAGS_CREATE_MODAL_HANDLER)
    fun createTag(
        event: ModalInteractionEvent,
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

    @JDASlashCommand(scope = CommandScope.GLOBAL_NO_DM, name = "tags", subcommand = "edit", description = "Edits a tag in this guild")
    suspend fun editTag(
        event: GuildSlashEvent,
        @AppOption(description = "Name of the tag", autocomplete = USER_TAGS_AUTOCOMPLETE) name: String
    ) {
        withOwnedTag(event, name) { tag: Tag ->
            val modal = Modals.create("Edit a tag", TAGS_EDIT_MODAL_HANDLER, name)
                .addActionRow(
                    Modals.createTextInput("tagName", "Tag name", TextInputStyle.SHORT)
                        .setValue(tag.name)
                        .build()
                )
                .addActionRow(
                    Modals.createTextInput("tagDescription", "Tag description", TextInputStyle.SHORT)
                        .setValue(tag.description)
                        .build()
                )
                .addActionRow(
                    Modals.createTextInput("tagContent", "Tag content", TextInputStyle.PARAGRAPH)
                        .setValue(tag.content)
                        .build()
                )
                .build()

            event.replyModal(modal).queue()
        }
    }

    @ModalHandler(name = TAGS_EDIT_MODAL_HANDLER)
    suspend fun editTag(
        event: ModalInteractionEvent,
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

    @JDASlashCommand(
        scope = CommandScope.GLOBAL_NO_DM,
        name = "tags",
        subcommand = "transfer",
        description = "Transfers a tag ownership to someone else in this guild"
    )
    suspend fun transferTag(
        event: GuildSlashEvent,
        @AppOption(description = "Name of the tag", autocomplete = USER_TAGS_AUTOCOMPLETE) name: String,
        @AppOption(description = "Member to transfer the tag to") newOwner: Member
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

    @JDASlashCommand(scope = CommandScope.GLOBAL_NO_DM, name = "tags", subcommand = "delete", description = "Deletes a tag you own in this guild")
    suspend fun deleteTag(
        event: GuildSlashEvent,
        @AppOption(description = "Name of the tag", autocomplete = USER_TAGS_AUTOCOMPLETE) name: String
    ) {
        withOwnedTag(event, name) { tag: Tag ->
            event.reply("Are you sure you want to delete the tag '${tag.name}' ?")
                .addActionRow(
                    *Components.group(
                        Components.dangerButton { btnEvt: ButtonEvent -> doDeleteTag(event, name, btnEvt) }
                            .build("Delete"),
                        Components.primaryButton { btnEvt: ButtonEvent ->
                            btnEvt.editMessage("Cancelled").setActionRows().queue()
                        }.build("No")
                    )
                )
                .setEphemeral(true)
                .queue()
        }
    }

    private fun doDeleteTag(event: GuildSlashEvent, name: String, btnEvt: ButtonEvent) {
        tagDB.delete(event.guild.idLong, event.user.idLong, name)
        btnEvt.editMessageFormat("Tag '%s' deleted successfully", name).setActionRows().queue()
    }

    @JDASlashCommand(scope = CommandScope.GLOBAL_NO_DM, name = "tags", subcommand = "list", description = "Creates a tag in this guild")
    fun listTags(
        event: GuildSlashEvent,
        @AppOption(name = "sorting", description = "Type of tag sorting") criteria: TagCriteria?
    ) {
        val finalCriteria = criteria ?: TagCriteria.NAME
        val totalTags = tagDB.getTotalTags(event.guild.idLong)
        val paginator = PaginatorBuilder()
            .setConstraints(InteractionConstraints.ofUsers(event.user))
            .setMaxPages(totalTags / 10)
            .setTimeout(5, TimeUnit.MINUTES) { p: Paginator, _ ->
                p.cleanup(event.context)
                event.hook.editOriginalComponents().queue()
            }
            .setPaginatorSupplier { _, _, _, page: Int ->
                try {
                    val tagRange = tagDB.getTagRange(event.guild.idLong, finalCriteria, 10 * page, 20)

                    val embed = Embed {
                        title = "All tags for " + event.guild.name

                        description = when {
                            tagRange.isEmpty() -> "No tags for this guild"
                            else -> tagRange.joinToString("\n") { t: Tag -> "${t.name} : ${t.description} : <@${t.ownerId}> (${t.uses} uses)" }
                        }
                    }

                    return@setPaginatorSupplier embed
                } catch (e: SQLException) {
                    LOGGER.error(
                        "An exception occurred while paginating through tags in guild '{}' ({})",
                        event.guild.name,
                        event.guild.idLong,
                        e
                    )
                    return@setPaginatorSupplier Embed(title = "Unable to get the tags")
                }
            }
            .build()
        event.reply(paginator.get()).setEphemeral(true).queue()
    }

    @JDASlashCommand(scope = CommandScope.GLOBAL_NO_DM, name = "tags", subcommand = "info", description = "Gives information about a tag in this guild")
    suspend fun infoTags(
        event: GuildSlashEvent,
        @AppOption(description = "Name of the tag", autocomplete = GUILD_TAGS_AUTOCOMPLETE) tagName: String
    ) {
        withTag(event, tagName) { tag: Tag ->
            event.deferReply(true).queue() //Retrieve might take some time, ig

            val embed = Embed {
                val ownerMember = event.guild.retrieveMemberById(tag.ownerId).await()
                if (ownerMember != null) {
                    author {
                        name = ownerMember.user.asTag
                        iconUrl = ownerMember.effectiveAvatarUrl
                    }
                } else {
                    val owner = event.jda.retrieveUserById(tag.ownerId).await()

                    author {
                        name = owner.asTag
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

    @AutocompletionHandler(name = GUILD_TAGS_AUTOCOMPLETE, showUserInput = false)
    fun guildTagsAutocomplete(event: CommandAutoCompleteInteractionEvent): List<Command.Choice> {
        val guild = checkGuild(event.guild)
        return AutocompleteAlgorithms
            .fuzzyMatching(
                tagDB.getShortTagsSorted(guild.idLong, TagCriteria.USES), { obj: ShortTag -> obj.name },
                event
            )
            .map { r: BoundExtractedResult<ShortTag> -> Command.Choice(r.referent.asChoiceName(), r.string) }
    }

    @AutocompletionHandler(name = USER_TAGS_AUTOCOMPLETE, showUserInput = false)
    fun userTagsAutocomplete(event: CommandAutoCompleteInteractionEvent): List<Command.Choice> {
        val guild = checkGuild(event.guild)
        return AutocompleteAlgorithms
            .fuzzyMatching(
                tagDB.getShortTagsSorted(guild.idLong, event.user.idLong, TagCriteria.NAME),
                { obj: ShortTag -> obj.name },
                event
            )
            .map { r: BoundExtractedResult<ShortTag> -> Command.Choice(r.referent.asChoiceName(), r.string) }
    }

    private fun ShortTag.asChoiceName(): String {
        val choiceName = "$name - $description"
        return when {
            choiceName.length > OptionData.MAX_CHOICE_NAME_LENGTH -> name
            //TODO maybe improve
            else -> choiceName
        }
    }
}