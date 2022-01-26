package com.freya02.bot.commands.slash.docs;

import com.freya02.bot.docs.CachedClass;
import com.freya02.bot.docs.DocIndexMap;
import com.freya02.bot.docs.index.DocIndex;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.docs.DocSourceType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ClassCommand extends ApplicationCommand {
	private final DocIndexMap docIndexMap;

	public ClassCommand() throws IOException {
		docIndexMap = DocIndexMap.getInstance();
	}

	@JDASlashCommand(
			name = "class",
			description = "Shows the documentation for a class"
	)
	public void onSlashClass(@NotNull GuildSlashEvent event,
	                         @NotNull @AppOption(description = "The docs to search upon")
			                         DocSourceType sourceType,
	                         @NotNull @AppOption(description = "Name of the Java class", autocomplete = CommonDocsHandlers.CLASS_NAME_AUTOCOMPLETE_NAME)
			                         String className) throws IOException {
		final DocIndex docIndex = docIndexMap.get(sourceType);
		final CachedClass cachedClass = docIndex.getClassDoc(className);

		if (cachedClass == null) {
			event.reply("Unknown class").setEphemeral(true).queue();

			return;
		}

		CommonDocsHandlers.sendClass(event, false, cachedClass);
	}
}