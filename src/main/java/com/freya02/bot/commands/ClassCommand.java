package com.freya02.bot.commands;

import com.freya02.bot.docs.*;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.AutocompletionHandler;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.botcommands.api.components.Components;
import com.freya02.botcommands.api.components.builder.LambdaSelectionMenuBuilder;
import com.freya02.botcommands.api.components.event.SelectionEvent;
import com.freya02.botcommands.api.utils.EmojiUtils;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.CommandAutoCompleteEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;

import java.io.IOException;
import java.util.List;

public class ClassCommand extends ApplicationCommand {
	private static final String SEE_ALSO_MENU_LISTENER_NAME = "seeAlsoMenu";
	private static final Emoji CLIPBOARD_EMOJI = EmojiUtils.resolveJDAEmoji("clipboard");
	private final DocsCollection docsCollection;
	private final List<String> simpleNameList;

	public ClassCommand(DocsCollection docsCollection) {
		this.docsCollection = docsCollection;

		this.simpleNameList = ClassReferences.getAllReferences().values()
				.stream()
				.map(ClassReference::className)
				.sorted()
				.toList();
	}

	@JDASlashCommand(name = "class")
	public void showClass(GuildSlashEvent event, @AppOption(description = "Name of the Java class", autocomplete = "autoClass") String className) throws IOException {
		final BasicDocs docs = docsCollection.docsMap().get(className);

		if (docs == null) {
			event.reply("Unknown class").setEphemeral(true).queue();

			return;
		}

		sendDocs(event, true, docs);
	}

	private void sendDocs(Interaction event, boolean ephemeral, BasicDocs docs) {
		ReplyAction replyAction = event.replyEmbeds(docs.toEmbed().build());

		final SeeAlso seeAlso = docs.getSeeAlso();
		if (seeAlso != null) {
			final List<SeeAlso.SeeAlsoItem> items = seeAlso.getItems();

			final LambdaSelectionMenuBuilder selectionMenuBuilder = Components.selectionMenu(evt -> onSeeAlsoClicked(evt, items));

			for (int i = 0, itemsSize = items.size(); i < itemsSize; i++) {
				SeeAlso.SeeAlsoItem item = items.get(i);

				selectionMenuBuilder.addOption(item.label(), String.valueOf(i), CLIPBOARD_EMOJI);
			}

			replyAction = replyAction.addActionRow(selectionMenuBuilder.build());
		}

		replyAction
				.setEphemeral(ephemeral)
				.queue();
	}

	@AutocompletionHandler(name = "autoClass")
	public List<String> autoClass(CommandAutoCompleteEvent event) {
		System.out.println("auto");

		return simpleNameList;
	}

	private void onSeeAlsoClicked(SelectionEvent event, List<SeeAlso.SeeAlsoItem> items) {
		try {
			final SeeAlso.SeeAlsoItem item = items.get(Integer.parseInt(event.getValues().get(0)));

			sendDocs(event, true, new BasicDocs(item.link()));
		} catch (IOException e) {
			event.reply("Couldn't send the docs").setEphemeral(true).queue();
		}
	}
}
