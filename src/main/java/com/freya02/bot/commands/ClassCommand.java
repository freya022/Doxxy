package com.freya02.bot.commands;

import com.freya02.bot.docs.DocIndex;
import com.freya02.botcommands.api.Logging;
import com.freya02.botcommands.api.annotations.Optional;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.CommandPath;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.annotations.Test;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.AutocompletionHandler;
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.CacheAutocompletion;
import com.freya02.botcommands.api.application.slash.autocomplete.annotations.CompositeKey;
import com.freya02.docs.DocSourceType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;

public class ClassCommand extends ApplicationCommand {
	private static final Logger LOGGER = Logging.getLogger();
//	private static final Emoji CLIPBOARD_EMOJI = EmojiUtils.resolveJDAEmoji("clipboard");
//	private static final int MAX_CHOICES = 25;
	private final EnumMap<DocSourceType, DocIndex> docIndexMap = new EnumMap<>(DocSourceType.class);

	public ClassCommand() throws IOException {
		docIndexMap.put(DocSourceType.BOT_COMMANDS, new DocIndex(DocSourceType.BOT_COMMANDS));
	}

	@Override
	@NotNull
	public List<Command.Choice> getOptionChoices(@Nullable Guild guild, @NotNull CommandPath commandPath, int optionIndex) {
		if (commandPath.equals(CommandPath.ofName("docs"))) {
			if (optionIndex == 0) { //sourceType
				return List.of(
						//TODO add more
						new Command.Choice("BotCommands", DocSourceType.BOT_COMMANDS.name())
				);
			}
		} else if (commandPath.equals(CommandPath.ofName("invalidate"))) {
			if (optionIndex == 0) {
				return List.of(
						new Command.Choice("autoClass", "autoClass"),
						new Command.Choice("autoMethod", "autoMethod")
				);
			}
		}

		return super.getOptionChoices(guild, commandPath, optionIndex);
	}

	@Test(guildIds = 722891685755093072L)
	@JDASlashCommand(
			name = "invalidate"
	)
	public void onSlashInvalidate(GuildSlashEvent event, @AppOption String name) {
		event.getContext().invalidateAutocompletionCache(name);
	}

	@JDASlashCommand(name = "docs")
	public void showDocs(GuildSlashEvent event,
						  @AppOption(description = "The docs to search upon") DocSourceType sourceType,
	                      @AppOption(description = "Name of the Java class", autocomplete = "autoClass") String className,
	                      @Optional @AppOption(description = "ID of the Java method for this class", autocomplete = "autoMethod") String methodId) throws IOException {

		final DocIndex docIndex = docIndexMap.get(sourceType);
		final MessageEmbed classDoc = docIndex.getClassDoc(className);

		if (classDoc == null) {
			event.reply("Unknown class").setEphemeral(true).queue();

			return;
		}

		if (methodId != null) {
			final MessageEmbed methodDoc = docIndex.getMethodDoc(className, methodId);

			if (methodDoc == null) {
				event.reply("Unknown method").setEphemeral(true).queue();

				return;
			}

			sendMethod(event, false, methodDoc);
		} else {
			sendClass(event, false, classDoc);
		}
	}

	private void sendMethod(GuildSlashEvent event, boolean ephemeral, MessageEmbed methodDoc) {
		ReplyCallbackAction replyAction = event.replyEmbeds(methodDoc);

		replyAction
				.setEphemeral(ephemeral)
				.queue();
	}

	private void sendClass(IReplyCallback event, boolean ephemeral, MessageEmbed docs) {
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

	@CacheAutocompletion
	@AutocompletionHandler(name = "autoClass", showUserInput = false)
	public Collection<String> autoClass(CommandAutoCompleteInteractionEvent event, @AppOption DocSourceType sourceType) {
		final DocIndex index = docIndexMap.get(sourceType);
		if (index == null) return List.of();

		return index.getSimpleNameList();
	}

	@CacheAutocompletion
	@AutocompletionHandler(name = "autoMethod", showUserInput = false)
	public Collection<String> autoMethod(CommandAutoCompleteInteractionEvent event,
	                                     @AppOption DocSourceType sourceType,
	                                     @CompositeKey @AppOption String className) {
		final DocIndex index = docIndexMap.get(sourceType);
		if (index == null) return List.of();

		final Collection<String> set = index.getMethodDocSuggestions(className);
		if (set == null) return List.of();

		return set;
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
}
