package com.freya02.bot.commands.slash.docs;

import com.freya02.bot.docs.index.DocIndex;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.docs.DocSourceType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ClassCommand extends BaseDocCommand {
	public ClassCommand() throws IOException {}

	@JDASlashCommand(
			name = "class",
			subcommand = "botcommands",
			description = "Shows the documentation for a class"
	)
	public void onSlashClassBC(@NotNull GuildSlashEvent event,
	                           @NotNull @AppOption(description = "The docs to search upon")
			                           DocSourceType sourceType,
	                           @NotNull @AppOption(description = "Name of the Java class", autocomplete = CommonDocsHandlers.CLASS_NAME_AUTOCOMPLETE_NAME)
			                           String className) throws IOException {
		onSlashClass(event, sourceType, className);
	}

	@JDASlashCommand(
			name = "class",
			subcommand = "jda",
			description = "Shows the documentation for a class"
	)
	public void onSlashClassJDA(@NotNull GuildSlashEvent event,
	                            @NotNull @AppOption(description = "The docs to search upon")
			                            DocSourceType sourceType,
	                            @NotNull @AppOption(description = "Name of the Java class", autocomplete = CommonDocsHandlers.CLASS_NAME_AUTOCOMPLETE_NAME)
			                            String className) throws IOException {
		onSlashClass(event, sourceType, className);
	}

	@JDASlashCommand(
			name = "class",
			subcommand = "java",
			description = "Shows the documentation for a class"
	)
	public void onSlashClassJava(@NotNull GuildSlashEvent event,
	                             @NotNull @AppOption(description = "The docs to search upon")
			                             DocSourceType sourceType,
	                             @NotNull @AppOption(description = "Name of the Java class", autocomplete = CommonDocsHandlers.CLASS_NAME_AUTOCOMPLETE_NAME)
			                             String className) throws IOException {
		onSlashClass(event, sourceType, className);
	}

	private void onSlashClass(@NotNull GuildSlashEvent event,
	                          @NotNull DocSourceType sourceType,
	                          @NotNull String className) throws IOException {
		final DocIndex docIndex = docIndexMap.get(sourceType);

		CommonDocsHandlers.handleClass(event, className, docIndex);
	}
}