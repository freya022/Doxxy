package com.freya02.bot.commands.slash.docs;

import com.freya02.bot.commands.slash.DeleteButtonListener;
import com.freya02.bot.docs.DocIndexMap;
import com.freya02.bot.docs.cached.CachedClass;
import com.freya02.bot.docs.cached.CachedDoc;
import com.freya02.bot.docs.cached.CachedField;
import com.freya02.bot.docs.cached.CachedMethod;
import com.freya02.bot.docs.index.DocIndex;
import com.freya02.botcommands.api.Logging;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.AutocompletionHandler;
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.CacheAutocompletion;
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.CompositeKey;
import com.freya02.botcommands.api.components.Components;
import com.freya02.botcommands.api.components.annotations.JDASelectionMenuListener;
import com.freya02.botcommands.api.components.builder.PersistentSelectionMenuBuilder;
import com.freya02.botcommands.api.components.event.SelectionEvent;
import com.freya02.botcommands.api.utils.EmojiUtils;
import com.freya02.docs.DocSourceType;
import com.freya02.docs.data.SeeAlso;
import com.freya02.docs.data.TargetType;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommonDocsHandlers extends ApplicationCommand {
	public static final String CLASS_NAME_AUTOCOMPLETE_NAME = "CommonDocsHandlers: className";
	public static final String CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME = "CommonDocsHandlers: classNameWithMethods";
	public static final String CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME = "CommonDocsHandlers: classNameWithFields";

	public static final String METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME = "CommonDocsHandlers: methodNameByClass";
	public static final String ANY_METHOD_NAME_AUTOCOMPLETE_NAME = "CommonDocsHandlers: anyMethodName";
	public static final String FIELD_NAME_AUTOCOMPLETE_NAME = "FieldCommand: fieldName";
	public static final String ANY_FIELD_NAME_AUTOCOMPLETE_NAME = "CommonDocsHandlers: anyFieldName";

	public static final String METHOD_NAME_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME = "CommonDocsHandlers: methodNameOrFieldByClass";

	public static final String SEE_ALSO_SELECT_LISTENER_NAME = "CommonDocsHandlers: seeAlso";

	public static final String[] AUTOCOMPLETE_NAMES = new String[]{
			CLASS_NAME_AUTOCOMPLETE_NAME,
			CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME,
			CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME,
			METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME,
			ANY_METHOD_NAME_AUTOCOMPLETE_NAME,
			FIELD_NAME_AUTOCOMPLETE_NAME,
			ANY_FIELD_NAME_AUTOCOMPLETE_NAME,
			METHOD_NAME_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME
	};

	private static final Logger LOGGER = Logging.getLogger();

	private final DocIndexMap docIndexMap;

	public CommonDocsHandlers() throws IOException {
		docIndexMap = DocIndexMap.getInstance();
	}

	static void sendClass(IReplyCallback event, boolean ephemeral, @NotNull CachedClass cachedClass) {
		ReplyCallbackAction replyAction = event.replyEmbeds(cachedClass.getClassEmbed());

		replyAction = addSeeAlso(cachedClass, replyAction);

		if (!ephemeral) replyAction = replyAction.addActionRow(DeleteButtonListener.getDeleteButton(event.getUser()));

		replyAction
				.setEphemeral(ephemeral)
				.queue();
	}

	static void sendMethod(IReplyCallback event, boolean ephemeral, @NotNull CachedMethod cachedMethod) {
		ReplyCallbackAction replyAction = event.replyEmbeds(cachedMethod.getMethodEmbed());

		replyAction = addSeeAlso(cachedMethod, replyAction);

		if (!ephemeral) replyAction = replyAction.addActionRow(DeleteButtonListener.getDeleteButton(event.getUser()));

		replyAction
				.setEphemeral(ephemeral)
				.queue();
	}

	static void sendField(IReplyCallback event, boolean ephemeral, @NotNull CachedField cachedField) {
		ReplyCallbackAction replyAction = event.replyEmbeds(cachedField.getFieldEmbed());

		replyAction = addSeeAlso(cachedField, replyAction);

		if (!ephemeral) replyAction = replyAction.addActionRow(DeleteButtonListener.getDeleteButton(event.getUser()));

		replyAction
				.setEphemeral(ephemeral)
				.queue();
	}

	static void handleClass(@NotNull GuildSlashEvent event, @NotNull String className, DocIndex docIndex) throws IOException {
		final CachedClass cachedClass = docIndex.getClassDoc(className);

		if (cachedClass == null) {
			event.reply("Unknown class").setEphemeral(true).queue();

			return;
		}

		sendClass(event, false, cachedClass);
	}

	static void handleMethodDocs(@NotNull GuildSlashEvent event, @AppOption(description = "Name of the Java class", autocomplete = CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME) @NotNull String className, @NotNull @AppOption(description = "Signature of the method / field name", autocomplete = METHOD_NAME_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME) String identifier, DocIndex docIndex) throws IOException {
		if (!docIndex.getClassesWithMethods().contains(className)) {
			event.reply("Unknown class").setEphemeral(true).queue();

			return;
		}

		final CachedMethod cachedMethod = docIndex.getMethodDoc(className, identifier);

		if (cachedMethod == null) {
			event.reply("Unknown method").setEphemeral(true).queue();

			return;
		}

		sendMethod(event, false, cachedMethod);
	}

	static void handleFieldDocs(@NotNull GuildSlashEvent event, @NotNull String className, @NotNull String identifier, DocIndex docIndex) throws IOException {
		if (!docIndex.getClassesWithFields().contains(className)) {
			event.reply("Unknown class").setEphemeral(true).queue();

			return;
		}

		final CachedField cachedField = docIndex.getFieldDoc(className, identifier);

		if (cachedField == null) {
			event.reply("Unknown field").setEphemeral(true).queue();

			return;
		}

		sendField(event, false, cachedField);
	}

	private static ReplyCallbackAction addSeeAlso(@NotNull CachedDoc cachedDoc, ReplyCallbackAction replyAction) {
		final List<SeeAlso.SeeAlsoReference> referenceList = cachedDoc.getSeeAlsoReferences();

		if (referenceList != null && referenceList.stream().anyMatch(s -> s.targetType() != TargetType.UNKNOWN)) {
			final PersistentSelectionMenuBuilder selectionMenuBuilder = Components.selectionMenu(SEE_ALSO_SELECT_LISTENER_NAME).timeout(15, TimeUnit.MINUTES);

			for (final SeeAlso.SeeAlsoReference reference : referenceList) {
				if (reference.targetType() != TargetType.UNKNOWN) {
					final String optionValue = reference.targetType().name() + ":" + reference.fullSignature();

					if (optionValue.length() > SelectMenu.ID_MAX_LENGTH) {
						LOGGER.warn("Option value was too large ({}) for: '{}'", optionValue.length(), optionValue);

						continue;
					}

					selectionMenuBuilder.addOption(reference.text(), optionValue, EmojiUtils.resolveJDAEmoji("clipboard"));
				}
			}

			selectionMenuBuilder.setPlaceholder("See also");

			return replyAction.addActionRow(selectionMenuBuilder.build());
		}

		return replyAction;
	}

	@JDASelectionMenuListener(name = SEE_ALSO_SELECT_LISTENER_NAME)
	public void onSeeAlsoSelect(SelectionEvent event) throws IOException {
		final SelectOption option = event.getSelectedOptions().get(0); //Forced to use 1

		final String[] values = option.getValue().split(":");

		final TargetType targetType = TargetType.valueOf(values[0]);
		final String fullSignature = values[1];

		for (DocIndex index : docIndexMap.values()) {
			final CachedDoc doc = switch (targetType) {
				case CLASS -> index.getClassDoc(fullSignature);
				case METHOD -> index.getMethodDoc(fullSignature);
				case FIELD -> index.getFieldDoc(fullSignature);
				default -> throw new IllegalArgumentException("Invalid target type: " + targetType);
			};

			if (doc != null) {
				switch (targetType) {
					case CLASS -> sendClass(event, true, (CachedClass) doc);
					case METHOD -> sendMethod(event, true, (CachedMethod) doc);
					case FIELD -> sendField(event, true, (CachedField) doc);
					default -> throw new IllegalArgumentException("Invalid target type: " + targetType);
				}

				break;
			}
		}
	}

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
	@AutocompletionHandler(name = ANY_FIELD_NAME_AUTOCOMPLETE_NAME, showUserInput = false)
	public Collection<String> onAnyFieldNameAutocomplete(CommandAutoCompleteInteractionEvent event,
	                                                     @CompositeKey @AppOption DocSourceType sourceType) {
		final DocIndex index = docIndexMap.get(sourceType);
		if (index == null) return List.of();

		return index.getFieldDocSuggestions();
	}

	@CacheAutocompletion
	@AutocompletionHandler(name = METHOD_NAME_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME, showUserInput = false)
	public Collection<String> onMethodNameOrFieldByClassAutocomplete(CommandAutoCompleteInteractionEvent event,
	                                                                 @CompositeKey @AppOption DocSourceType sourceType,
	                                                                 @CompositeKey @AppOption String className) {
		final DocIndex index = docIndexMap.get(sourceType);
		if (index == null) return List.of();

		return index.getMethodAndFieldDocSuggestions(className);
	}
}
