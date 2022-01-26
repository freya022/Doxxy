package com.freya02.bot.commands.slash.docs;

import com.freya02.bot.docs.CachedField;
import com.freya02.bot.docs.DocIndexMap;
import com.freya02.bot.docs.index.DocIndex;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.docs.DocSourceType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class FieldCommand extends ApplicationCommand {
	private final DocIndexMap docIndexMap;

	public FieldCommand() throws IOException {
		docIndexMap = DocIndexMap.getInstance();
	}

	@JDASlashCommand(
			name = "field",
			description = "Shows the documentation for a field of a class"
	)
	public void onSlashField(@NotNull GuildSlashEvent event,
	                         @NotNull @AppOption(description = "The docs to search upon")
			                         DocSourceType sourceType,
	                         @NotNull @AppOption(description = "The class to search the field in", autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME)
			                         String className,
	                         @NotNull @AppOption(description = "Name of the field", autocomplete = CommonDocsHandlers.FIELD_NAME_AUTOCOMPLETE_NAME)
			                         String fieldName) throws IOException {

		final DocIndex docIndex = docIndexMap.get(sourceType);

		if (!docIndex.getClassesWithFields().contains(className)) {
			event.reply("Unknown class").setEphemeral(true).queue();

			return;
		}

		final CachedField cachedField = docIndex.getFieldDoc(className, fieldName);

		if (cachedField == null) {
			event.reply("Unknown field").setEphemeral(true).queue();

			return;
		}

		CommonDocsHandlers.sendField(event, false, cachedField);
	}
}