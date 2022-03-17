package com.freya02.bot.commands.slash.docs;

import com.freya02.bot.docs.cached.CachedField;
import com.freya02.bot.docs.index.DocIndex;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.docs.DocSourceType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class AnyFieldCommand extends BaseDocCommand {
	public AnyFieldCommand() throws IOException {}

	@JDASlashCommand(
			name = "anyfield",
			subcommand = "botcommands",
			description = "Shows the documentation for any field"
	)
	public void onSlashAnyFieldBC(@NotNull GuildSlashEvent event,
	                              @NotNull @AppOption(description = "The docs to search upon")
			                              DocSourceType sourceType,
	                              @NotNull @AppOption(description = "Full signature of the class + field", autocomplete = CommonDocsHandlers.ANY_FIELD_NAME_AUTOCOMPLETE_NAME)
			                              String fullSignature) throws IOException {
		onSlashAnyField(event, sourceType, fullSignature);
	}

	@JDASlashCommand(
			name = "anyfield",
			subcommand = "jda",
			description = "Shows the documentation for any field"
	)
	public void onSlashAnyFieldJDA(@NotNull GuildSlashEvent event,
	                               @NotNull @AppOption(description = "The docs to search upon")
			                               DocSourceType sourceType,
	                               @NotNull @AppOption(description = "Full signature of the class + field", autocomplete = CommonDocsHandlers.ANY_FIELD_NAME_AUTOCOMPLETE_NAME)
			                               String fullSignature) throws IOException {
		onSlashAnyField(event, sourceType, fullSignature);
	}

	@JDASlashCommand(
			name = "anyfield",
			subcommand = "java",
			description = "Shows the documentation for any field"
	)
	public void onSlashAnyFieldJava(@NotNull GuildSlashEvent event,
	                                @NotNull @AppOption(description = "The docs to search upon")
			                                DocSourceType sourceType,
	                                @NotNull @AppOption(description = "Full signature of the class + field", autocomplete = CommonDocsHandlers.ANY_FIELD_NAME_AUTOCOMPLETE_NAME)
			                                String fullSignature) throws IOException {
		onSlashAnyField(event, sourceType, fullSignature);
	}

	private void onSlashAnyField(@NotNull GuildSlashEvent event,
	                             @NotNull DocSourceType sourceType,
	                             @NotNull String fullSignature) throws IOException {

		final DocIndex docIndex = docIndexMap.get(sourceType);

		final CachedField cachedField = docIndex.getFieldDoc(fullSignature);

		if (cachedField == null) {
			event.reply("Unknown field").setEphemeral(true).queue();

			return;
		}

		CommonDocsHandlers.sendField(event, false, cachedField);
	}
}
