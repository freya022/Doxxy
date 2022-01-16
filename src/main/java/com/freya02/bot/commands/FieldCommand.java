package com.freya02.bot.commands;

import com.freya02.bot.docs.DocIndex;
import com.freya02.bot.docs.DocIndexMap;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.AutocompletionHandler;
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.CacheAutocompletion;
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.CompositeKey;
import com.freya02.docs.DocSourceType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class FieldCommand extends ApplicationCommand {
	private static final String CLASS_NAME_AUTOCOMPLETE_NAME = "FieldCommand: className";
	private static final String FIELD_NAME_AUTOCOMPLETE_NAME = "FieldCommand: fieldName";

	private final DocIndexMap docIndexMap;

	public FieldCommand() throws IOException {
		docIndexMap = DocIndexMap.getInstance();
	}

	@JDASlashCommand(
			name = "field",
			description = "Searches for a field on a class"
	)
	public void onSlashField(GuildSlashEvent event,
	                         @AppOption(description = "The docs to search upon") DocSourceType sourceType,
	                         @AppOption(description = "The class to search the field in", autocomplete = CLASS_NAME_AUTOCOMPLETE_NAME) String className,
	                         @AppOption(description = "The field to search for", autocomplete = FIELD_NAME_AUTOCOMPLETE_NAME) String fieldName) throws IOException {
		final DocIndex docIndex = docIndexMap.get(sourceType);
		final MessageEmbed classDoc = docIndex.getClassDoc(className);

		if (classDoc == null) {
			event.reply("Unknown class").setEphemeral(true).queue();

			return;
		}

		if (fieldName != null) {
			final MessageEmbed fieldDoc = docIndex.getFieldDoc(className, fieldName);

			if (fieldDoc == null) {
				event.reply("Unknown field").setEphemeral(true).queue();

				return;
			}

			sendField(event, false, fieldDoc);
		} else {
			ClassCommand.sendClass(event, false, classDoc);
		}
	}

	private void sendField(GuildSlashEvent event, boolean ephemeral, MessageEmbed fieldDoc) {
		ReplyCallbackAction replyAction = event.replyEmbeds(fieldDoc);

		replyAction
				.setEphemeral(ephemeral)
				.queue();
	}

	@CacheAutocompletion
	@AutocompletionHandler(name = CLASS_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
	public Collection<String> onClassNameAutocomplete(CommandAutoCompleteInteractionEvent event, @AppOption DocSourceType sourceType) {
		final DocIndex index = docIndexMap.get(sourceType);
		if (index == null) return List.of();

		return index.getSimpleNameList();
	}

	@CacheAutocompletion
	@AutocompletionHandler(name = FIELD_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
	public Collection<String> onFieldNameAutocomplete(CommandAutoCompleteInteractionEvent event,
	                                                  @AppOption DocSourceType sourceType,
	                                                  @CompositeKey @AppOption String className) {
		final DocIndex index = docIndexMap.get(sourceType);
		if (index == null) return List.of();

		final Collection<String> set = index.getFieldDocSuggestions(className);
		if (set == null) return List.of();

		return set;
	}
}