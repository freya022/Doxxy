package com.freya02.bot.commands.slash;

import com.freya02.bot.db.Database;
import com.freya02.bot.db.SQLCodes;
import com.freya02.bot.tag.*;
import com.freya02.botcommands.api.Logging;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.botcommands.api.application.slash.autocomplete.AutocompleteAlgorithms;
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.AutocompletionHandler;
import com.freya02.botcommands.api.components.Components;
import com.freya02.botcommands.api.components.InteractionConstraints;
import com.freya02.botcommands.api.components.event.ButtonEvent;
import com.freya02.botcommands.api.modals.Modals;
import com.freya02.botcommands.api.modals.annotations.ModalData;
import com.freya02.botcommands.api.modals.annotations.ModalHandler;
import com.freya02.botcommands.api.modals.annotations.ModalInput;
import com.freya02.botcommands.api.pagination.paginator.Paginator;
import com.freya02.botcommands.api.pagination.paginator.PaginatorBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.text.Modal;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.Permission.MANAGE_ROLES;
import static net.dv8tion.jda.api.Permission.MANAGE_SERVER;

public class SlashTag extends ApplicationCommand {
	private static final Logger LOGGER = Logging.getLogger();

	private static final String GUILD_TAGS_AUTOCOMPLETE = "guildTagsAutocomplete";
	private static final String USER_TAGS_AUTOCOMPLETE = "userTagsAutocomplete";

	private static final String TAGS_CREATE_MODAL_HANDLER = "SlashTag: tagsCreate";
	private static final String TAGS_EDIT_MODAL_HANDLER = "SlashTag: tagsEdit";

	private final TagDB tagDB;

	public SlashTag(Database database) throws SQLException {
		this.tagDB = new TagDB(database);
	}

	@Contract(value = "null -> fail", pure = true)
	private <T> T checkGuild(@Nullable T obj) {
		if (obj == null)
			throw new IllegalArgumentException("Event did not happen in a guild");

		return obj;
	}

	private void withOwnedTag(@NotNull IReplyCallback event, @NotNull String name, TagConsumer consumer) throws SQLException {
		final Tag tag = tagDB.get(checkGuild(event.getGuild()).getIdLong(), name);

		if (tag == null) {
			event.reply("Tag '" + name + "' was not found").setEphemeral(true).queue();

			return;
		} else if (tag.ownerId() != event.getUser().getIdLong()
				&& !checkGuild(event.getMember()).hasPermission(event.getGuildChannel(), MANAGE_SERVER, MANAGE_ROLES)) {
			event.reply("You do not own this tag").setEphemeral(true).queue();

			return;
		}

		consumer.accept(tag);
	}

	private void withTag(@NotNull GuildSlashEvent event, @NotNull String name, TagConsumer consumer) throws SQLException {
		final Tag tag = tagDB.get(event.getGuild().getIdLong(), name);

		if (tag == null) {
			event.reply("Tag '" + name + "' was not found").setEphemeral(true).queue();

			return;
		}

		consumer.accept(tag);
	}

	@JDASlashCommand(name = "tag", description = "Sends a predefined tag")
	public void sendTag(GuildSlashEvent event,
	                    @AppOption(description = "Name of the tag", autocomplete = GUILD_TAGS_AUTOCOMPLETE) String name) throws SQLException {

		withTag(event, name, tag -> {
			tagDB.incrementTag(event.getGuild().getIdLong(), tag.name());

			event.reply(tag.content()).queue();
		});
	}

	@JDASlashCommand(name = "tags", subcommand = "raw", description = "Sends a predefined tag with all the markdown escaped")
	public void sendRawTag(GuildSlashEvent event,
	                       @AppOption(description = "Name of the tag", autocomplete = GUILD_TAGS_AUTOCOMPLETE) String name) throws SQLException {

		withTag(event, name, tag -> event.reply(MarkdownSanitizer.escape(tag.content())).queue());
	}

	@JDASlashCommand(name = "tags", subcommand = "create", description = "Creates a tag in this guild")
	public void createTag(GuildSlashEvent event) {
		final Modal modal = Modals.create("Create a tag", TAGS_CREATE_MODAL_HANDLER)
				.addActionRow(Modals.createTextInput("tagName", "Tag name", TextInputStyle.SHORT).build())
				.addActionRow(Modals.createTextInput("tagDescription", "Tag description", TextInputStyle.SHORT).build())
				.addActionRow(Modals.createTextInput("tagContent", "Tag content", TextInputStyle.PARAGRAPH).build())
				.build();

		event.replyModal(modal).queue();
	}

	@ModalHandler(name = TAGS_CREATE_MODAL_HANDLER)
	public void createTag(ModalInteractionEvent event,
	                      @ModalInput(name = "tagName") String name,
	                      @ModalInput(name = "tagDescription") String description,
	                      @ModalInput(name = "tagContent") String content
	) throws SQLException {
		try {
			tagDB.create(checkGuild(event.getGuild()).getIdLong(), event.getUser().getIdLong(), name, description, content);

			event.replyFormat("Tag '%s' created successfully", name).setEphemeral(true).queue();
		} catch (SQLException e) {
			if (SQLCodes.isUniqueViolation(e)) {
				event.replyFormat("Tag '%s' already exists", name).setEphemeral(true).queue();
			} else {
				throw e;
			}
		} catch (TagException e) {
			event.reply(e.getMessage()).setEphemeral(true).queue();
		}
	}

	@JDASlashCommand(name = "tags", subcommand = "edit", description = "Edits a tag in this guild")
	public void editTag(GuildSlashEvent event,
	                    @AppOption(description = "Name of the tag", autocomplete = USER_TAGS_AUTOCOMPLETE) String name) throws SQLException {

		withOwnedTag(event, name, tag -> {
			final Modal modal = Modals.create(TAGS_EDIT_MODAL_HANDLER, name)
					.setTitle("Edit a tag")
					.addActionRow(Modals.createTextInput("tagName", "Tag name", TextInputStyle.SHORT)
							.setValue(tag.name())
							.build())
					.addActionRow(Modals.createTextInput("tagDescription", "Tag description", TextInputStyle.SHORT)
							.setValue(tag.description())
							.build())
					.addActionRow(Modals.createTextInput("tagContent", "Tag content", TextInputStyle.PARAGRAPH)
							.setValue(tag.content())
							.build())
					.build();

			event.replyModal(modal).queue();
		});
	}

	@ModalHandler(name = TAGS_EDIT_MODAL_HANDLER)
	public void editTag(ModalInteractionEvent event,
						  @ModalData String name,
	                      @ModalInput(name = "tagName") String newName,
	                      @ModalInput(name = "tagDescription") String newDescription,
	                      @ModalInput(name = "tagContent") String newContent
	) throws SQLException {
		withOwnedTag(event, name, tag -> {
			try {
				tagDB.edit(checkGuild(event.getGuild()).getIdLong(), event.getUser().getIdLong(), name, newName, newDescription, newContent);

				event.replyFormat("Tag '%s' edited successfully", name).setEphemeral(true).queue();
			} catch (TagException e) {
				event.reply(e.getMessage()).setEphemeral(true).queue();
			}
		});
	}

	@JDASlashCommand(name = "tags", subcommand = "transfer", description = "Transfers a tag ownership to someone else in this guild")
	public void transferTag(GuildSlashEvent event,
	                        @NotNull @AppOption(description = "Name of the tag", autocomplete = USER_TAGS_AUTOCOMPLETE) String name,
	                        @NotNull @AppOption(description = "Member to transfer the tag to") Member newOwner) throws SQLException {

		if (newOwner.getUser().isBot()) {
			event.reply("The member to transfer the tag to cannot be a bot").setEphemeral(true).queue();

			return;
		}

		withOwnedTag(event, name, tag -> {
			tagDB.transfer(event.getGuild().getIdLong(),
					event.getUser().getIdLong(),
					name,
					newOwner.getIdLong());

			event.replyFormat("Tag '%s' transfer to %s successfully", name, newOwner.getAsMention())
					.setEphemeral(true)
					.queue();
		});
	}

	@JDASlashCommand(name = "tags", subcommand = "delete", description = "Deletes a tag you own in this guild")
	public void deleteTag(GuildSlashEvent event,
	                      @AppOption(description = "Name of the tag", autocomplete = USER_TAGS_AUTOCOMPLETE) String name) throws SQLException {

		withOwnedTag(event, name, tag -> {
			event.reply("Are you sure you want to delete the tag '" + tag.name() + "' ?")
					.addActionRow(Components.group(
							Components.dangerButton(btnEvt -> doDeleteTag(event, name, btnEvt)).build("Delete"),
							Components.primaryButton(btnEvt -> btnEvt.editMessage("Cancelled").setActionRows().queue()).build("No")
					))
					.setEphemeral(true)
					.queue();
		});
	}

	private void doDeleteTag(GuildSlashEvent event, String name, ButtonEvent btnEvt) throws SQLException {
		tagDB.delete(event.getGuild().getIdLong(), event.getUser().getIdLong(), name);

		btnEvt.editMessageFormat("Tag '%s' deleted successfully", name).setActionRows().queue();
	}

	@JDASlashCommand(name = "tags", subcommand = "list", description = "Creates a tag in this guild")
	public void listTags(GuildSlashEvent event,
	                     @Nullable @AppOption(name = "sorting", description = "Type of tag sorting") TagCriteria criteria) throws SQLException {

		final TagCriteria finalCriteria = criteria == null ? TagCriteria.NAME : criteria;

		final int totalTags = tagDB.getTotalTags(event.getGuild().getIdLong());

		final Paginator paginator = new PaginatorBuilder()
				.setConstraints(InteractionConstraints.ofUsers(event.getUser()))
				.setMaxPages(totalTags / 10)
				.setTimeout(5, TimeUnit.MINUTES, (p, message) -> {
					p.cleanup(event.getContext());

					event.getHook().editOriginalComponents().queue();
				})
				.setPaginatorSupplier((inst, messageBuilder, components, page) -> {
					try {
						final List<Tag> tagRange = tagDB.getTagRange(event.getGuild().getIdLong(), finalCriteria, 10 * page, 20);

						final EmbedBuilder builder = new EmbedBuilder()
								.setTitle("All tags for " + event.getGuild().getName());

						if (tagRange.isEmpty()) {
							builder.setDescription("No tags for this guild");
						} else {
							builder.setDescription(tagRange.stream()
									.map(t -> t.name() + " : " + t.description() + " : <@" + t.ownerId() + "> (" + t.uses() + " uses)")
									.collect(Collectors.joining("\n")));
						}

						return builder.build();
					} catch (SQLException e) {
						LOGGER.error("An exception occurred while paginating through tags in guild '{}' ({})", event.getGuild().getName(), event.getGuild().getIdLong(), e);

						return new EmbedBuilder().setTitle("Unable to get the tags").build();
					}
				})
				.build();

		event.reply(paginator.get()).setEphemeral(true).queue();
	}

	@JDASlashCommand(name = "tags", subcommand = "info", description = "Gives information about a tag in this guild")
	public void infoTags(GuildSlashEvent event,
	                     @AppOption(description = "Name of the tag", autocomplete = GUILD_TAGS_AUTOCOMPLETE) String name) throws SQLException {

		withTag(event, name, tag -> {
			final EmbedBuilder builder = new EmbedBuilder();

			event.deferReply(true).queue(); //Retrieve might take some time, ig

			final Member ownerMember = event.getGuild().retrieveMemberById(tag.ownerId()).complete();
			if (ownerMember != null) {
				builder.setAuthor(ownerMember.getUser().getAsTag(), null, ownerMember.getEffectiveAvatarUrl());
			} else {
				final User owner = event.getJDA().retrieveUserById(tag.ownerId()).complete();

				builder.setAuthor(owner.getAsTag(), null, owner.getEffectiveAvatarUrl());
			}

			builder.setTitle("Tag '" + tag.name() + "'");
			builder.addField("Description", tag.description(), false);
			builder.addField("Owner", "<@" + tag.ownerId() + ">", true);
			builder.addField("Uses", String.valueOf(tag.uses()), true);
			builder.addField("Rank", String.valueOf(tagDB.getRank(event.getGuild().getIdLong(), name)), true);
			builder.setFooter("Created on").setTimestamp(tag.createdAt());

			event.getHook().sendMessageEmbeds(builder.build()).setEphemeral(true).queue();
		});
	}

	@AutocompletionHandler(name = GUILD_TAGS_AUTOCOMPLETE, showUserInput = false)
	public List<Command.Choice> guildTagsAutocomplete(CommandAutoCompleteInteractionEvent event) throws SQLException {
		final Guild guild = event.getGuild();
		if (guild == null) throw new IllegalStateException("Tag autocompletion was triggered outside of a Guild");

		return AutocompleteAlgorithms
				.fuzzyMatching(tagDB.getShortTagsSorted(guild.getIdLong(), TagCriteria.USES),
						ShortTag::name,
						event)
				.stream()
				.map(r -> new Command.Choice(getChoiceName(r.getReferent()), r.getString()))
				.toList();
	}

	@AutocompletionHandler(name = USER_TAGS_AUTOCOMPLETE, showUserInput = false)
	public List<Command.Choice> userTagsAutocomplete(CommandAutoCompleteInteractionEvent event) throws SQLException {
		final Guild guild = event.getGuild();
		if (guild == null) throw new IllegalStateException("Tag autocompletion was triggered outside of a Guild");

		return AutocompleteAlgorithms
				.fuzzyMatching(tagDB.getShortTagsSorted(guild.getIdLong(), event.getUser().getIdLong(), TagCriteria.NAME),
						ShortTag::name,
						event)
				.stream()
				.map(r -> new Command.Choice(getChoiceName(r.getReferent()), r.getString()))
				.toList();
	}

	@NotNull
	private String getChoiceName(ShortTag shortTag) {
		final String choiceName = shortTag.name() + " - " + shortTag.description();
		if (choiceName.length() > OptionData.MAX_CHOICE_NAME_LENGTH) {
			return shortTag.name(); //TODO maybe improve
		}

		return choiceName;
	}

	private interface TagConsumer {
		void accept(Tag tag) throws SQLException;
	}
}