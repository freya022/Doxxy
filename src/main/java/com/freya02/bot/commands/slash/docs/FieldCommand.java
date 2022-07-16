package com.freya02.bot.commands.slash.docs;

import com.freya02.bot.docs.index.DocIndex;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.docs.DocSourceType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class FieldCommand extends BaseDocCommand {
	public FieldCommand() throws IOException {}

	@JDASlashCommand(
			name = "field",
			subcommand = "botcommands",
			description = "Shows the documentation for a field of a class"
	)
	public void onSlashFieldBC(@NotNull GuildSlashEvent event,
	                           @NotNull @AppOption(description = "The docs to search upon")
			                           DocSourceType sourceType,
	                           @NotNull @AppOption(description = "The class to search the field in", autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME)
			                           String className,
	                           @NotNull @AppOption(description = "Name of the field", autocomplete = CommonDocsHandlers.FIELD_NAME_AUTOCOMPLETE_NAME)
			                           String fieldName) throws IOException {
		onSlashField(event, sourceType, className, fieldName);
	}

	@JDASlashCommand(
			name = "field",
			subcommand = "jda",
			description = "Shows the documentation for a field of a class"
	)
	public void onSlashFieldJDA(@NotNull GuildSlashEvent event,
	                            @NotNull @AppOption(description = "The docs to search upon")
			                            DocSourceType sourceType,
	                            @NotNull @AppOption(description = "The class to search the field in", autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME)
			                            String className,
	                            @NotNull @AppOption(description = "Name of the field", autocomplete = CommonDocsHandlers.FIELD_NAME_AUTOCOMPLETE_NAME)
			                            String fieldName) throws IOException {
		onSlashField(event, sourceType, className, fieldName);
	}

	@JDASlashCommand(
			name = "field",
			subcommand = "java",
			description = "Shows the documentation for a field of a class"
	)
	public void onSlashFieldJava(@NotNull GuildSlashEvent event,
	                             @NotNull @AppOption(description = "The docs to search upon")
			                             DocSourceType sourceType,
	                             @NotNull @AppOption(description = "The class to search the field in", autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_FIELDS_AUTOCOMPLETE_NAME)
			                             String className,
	                             @NotNull @AppOption(description = "Name of the field", autocomplete = CommonDocsHandlers.FIELD_NAME_AUTOCOMPLETE_NAME)
			                             String fieldName) throws IOException {
		onSlashField(event, sourceType, className, fieldName);
	}

	private void onSlashField(@NotNull GuildSlashEvent event,
	                          @NotNull DocSourceType sourceType,
	                          @NotNull String className,
	                          @NotNull String fieldName) throws IOException {

		final DocIndex docIndex = docIndexMap.get(sourceType);

		CommonDocsHandlers.handleFieldDocs(event, className, fieldName, docIndex);
	}
}