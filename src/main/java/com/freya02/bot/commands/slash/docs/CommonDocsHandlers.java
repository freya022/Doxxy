package com.freya02.bot.commands.slash.docs;

import com.freya02.bot.docs.DocIndexMap;
import com.freya02.bot.docs.index.DocIndex;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.AutocompletionHandler;
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.CacheAutocompletion;
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.CompositeKey;
import com.freya02.docs.DocSourceType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class CommonDocsHandlers extends ApplicationCommand {
	public static final String CLASS_NAME_AUTOCOMPLETE_NAME = "CommonDocsHandlers: className";
	public static final String CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME = "CommonDocsHandlers: classNameWithMethods";
	public static final String CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME = "CommonDocsHandlers: classNameWithFields";

	public static final String METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME = "CommonDocsHandlers: methodNameByClass";
	public static final String ANY_METHOD_NAME_AUTOCOMPLETE_NAME = "CommonDocsHandlers: anyMethodName";
	public static final String FIELD_NAME_AUTOCOMPLETE_NAME = "FieldCommand: fieldName";
	public static final String ANY_FIELD_NAME_AUTOCOMPLETE_NAME = "CommonDocsHandlers: anyFieldName";

	public static final String[] AUTOCOMPLETE_NAMES = new String[]{
			CLASS_NAME_AUTOCOMPLETE_NAME,
			CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME,
			CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME,
			METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME,
			ANY_METHOD_NAME_AUTOCOMPLETE_NAME,
			FIELD_NAME_AUTOCOMPLETE_NAME,
			ANY_FIELD_NAME_AUTOCOMPLETE_NAME
	};

	private final DocIndexMap docIndexMap;

	public CommonDocsHandlers() throws IOException {
		docIndexMap = DocIndexMap.getInstance();
	}

	static void sendMethod(GuildSlashEvent event, boolean ephemeral, MessageEmbed methodDoc) {
		ReplyCallbackAction replyAction = event.replyEmbeds(methodDoc);

		replyAction
				.setEphemeral(ephemeral)
				.queue();
	}

	static void sendClass(IReplyCallback event, boolean ephemeral, MessageEmbed docs) {
		ReplyCallbackAction replyAction = event.replyEmbeds(docs);

		// Much more work to do for this to really work
		// The link could target method without knowing it, it could also target weird internal sun classes
//		final SeeAlso seeAlso = docs.getSeeAlso();
//		if (seeAlso != null) {
//			final List<SeeAlso.SeeAlsoReference> references = seeAlso.getReferences();
//			final LambdaSelectionMenuBuilder selectionMenuBuilder = Components.selectionMenu(evt -> onSeeAlsoClicked(evt, references));
//
//			for (int i = 0, referencesSize = Math.min(MAX_CHOICES, references.size()); i < referencesSize; i++) {
//				SeeAlso.SeeAlsoReference reference = references.get(i);
//
//				final ClassDoc docOpt = ClassDocs.getOrNull(reference.link());
//				if (docOpt != null) {
//					selectionMenuBuilder.addOption(reference.text(), String.valueOf(i), CLIPBOARD_EMOJI);
//				}
//			}
//
//			replyAction = replyAction.addActionRow(selectionMenuBuilder.build());
//		}

		replyAction
				.setEphemeral(ephemeral)
				.queue();
	}

	static void sendField(GuildSlashEvent event, boolean ephemeral, MessageEmbed fieldDoc) {
		ReplyCallbackAction replyAction = event.replyEmbeds(fieldDoc);

		replyAction
				.setEphemeral(ephemeral)
				.queue();
	}

//	private void onSeeAlsoClicked(SelectionEvent event, List<SeeAlso.SeeAlsoReference> references) {
//		try {
//			final SeeAlso.SeeAlsoReference reference = references.get(Integer.parseInt(event.getValues().get(0)));
//
//			sendDocs(event, true, ClassDocs.of(reference.link()));
//		} catch (IOException e) {
//			event.reply("Couldn't send the docs").setEphemeral(true).queue();
//		}
//	}

	@CacheAutocompletion
	@AutocompletionHandler(name = CLASS_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
	public Collection<String> onClassNameAutocomplete(CommandAutoCompleteInteractionEvent event,
	                                                  @CompositeKey @AppOption DocSourceType sourceType) {
		final DocIndex index = docIndexMap.get(sourceType);
		if (index == null) return List.of();

		return index.getSimpleNameList();
	}

	@CacheAutocompletion
	@AutocompletionHandler(name = CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME, showUserInput = false)
	public Collection<String> onClassNameWithMethodsAutocomplete(CommandAutoCompleteInteractionEvent event,
	                                                             @CompositeKey @AppOption DocSourceType sourceType) {
		final DocIndex index = docIndexMap.get(sourceType);
		if (index == null) return List.of();

		return index.getClassesWithMethods();
	}

	@CacheAutocompletion
	@AutocompletionHandler(name = CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME, showUserInput = false)
	public Collection<String> onClassNameWithFieldsAutocomplete(CommandAutoCompleteInteractionEvent event,
	                                                            @CompositeKey @AppOption DocSourceType sourceType) {
		final DocIndex index = docIndexMap.get(sourceType);
		if (index == null) return List.of();

		return index.getClassesWithFields();
	}

	@CacheAutocompletion
	@AutocompletionHandler(name = METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME, showUserInput = false)
	public Collection<String> onMethodNameByClassAutocomplete(CommandAutoCompleteInteractionEvent event,
	                                                          @CompositeKey @AppOption DocSourceType sourceType,
	                                                          @CompositeKey @AppOption String className) {
		final DocIndex index = docIndexMap.get(sourceType);
		if (index == null) return List.of();

		return index.getMethodDocSuggestions(className);
	}

	@CacheAutocompletion
	@AutocompletionHandler(name = ANY_METHOD_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
	public Collection<String> onAnyMethodNameAutocomplete(CommandAutoCompleteInteractionEvent event,
	                                                      @CompositeKey @AppOption DocSourceType sourceType) {
		final DocIndex index = docIndexMap.get(sourceType);
		if (index == null) return List.of();

		final Collection<String> set = index.getMethodDocSuggestions();

		//TODO real fix hopefully
		return set.stream().filter(s -> s.length() <= OptionData.MAX_CHOICE_VALUE_LENGTH).toList();
	}

	@CacheAutocompletion
	@AutocompletionHandler(name = FIELD_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
	public Collection<String> onFieldNameAutocomplete(CommandAutoCompleteInteractionEvent event,
	                                                  @CompositeKey @AppOption DocSourceType sourceType,
	                                                  @CompositeKey @AppOption String className) {
		final DocIndex index = docIndexMap.get(sourceType);
		if (index == null) return List.of();

		return index.getFieldDocSuggestions(className);
	}

	@CacheAutocompletion
	@AutocompletionHandler(name = ANY_FIELD_NAME_AUTOCOMPLETE_NAME)
	public Collection<String> onAnyFieldNameAutocomplete(CommandAutoCompleteInteractionEvent event,
	                                                     @CompositeKey @AppOption DocSourceType sourceType) {
		final DocIndex index = docIndexMap.get(sourceType);
		if (index == null) return List.of();

		return index.getFieldDocSuggestions();
	}
}
