package com.freya02.bot.commands.slash.docs;

import com.freya02.bot.docs.index.DocIndex;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.docs.DocSourceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class DocsCommand extends BaseDocCommand {
	public DocsCommand() throws IOException {}

	@JDASlashCommand(
			name = "docs",
			subcommand = "botcommands",
			description = "Shows the documentation for a class, a method or a field"
	)
	public void onSlashDocsBC(@NotNull GuildSlashEvent event,
	                          @NotNull @AppOption(description = "The docs to search upon")
			                          DocSourceType sourceType,
	                          @NotNull @AppOption(description = "Name of the Java class", autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME)
			                          String className,
	                          @Nullable @AppOption(description = "Signature of the method / field name", autocomplete = CommonDocsHandlers.METHOD_NAME_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME)
			                          String identifier) throws IOException {
		onSlashDocs(event, sourceType, className, identifier);
	}

	@JDASlashCommand(
			name = "docs",
			subcommand = "jda",
			description = "Shows the documentation for a class, a method or a field"
	)
	public void onSlashDocsJDA(@NotNull GuildSlashEvent event,
	                           @NotNull @AppOption(description = "The docs to search upon")
			                           DocSourceType sourceType,
	                           @NotNull @AppOption(description = "Name of the Java class", autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME)
			                           String className,
	                           @Nullable @AppOption(description = "Signature of the method / field name", autocomplete = CommonDocsHandlers.METHOD_NAME_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME)
			                           String identifier) throws IOException {
		onSlashDocs(event, sourceType, className, identifier);
	}

	@JDASlashCommand(
			name = "docs",
			subcommand = "java",
			description = "Shows the documentation for a class, a method or a field"
	)
	public void onSlashDocsJava(@NotNull GuildSlashEvent event,
	                            @NotNull @AppOption(description = "The docs to search upon")
			                            DocSourceType sourceType,
	                            @NotNull @AppOption(description = "Name of the Java class", autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME)
			                            String className,
	                            @Nullable @AppOption(description = "Signature of the method / field name", autocomplete = CommonDocsHandlers.METHOD_NAME_OR_FIELD_BY_CLASS_AUTOCOMPLETE_NAME)
			                            String identifier) throws IOException {
		onSlashDocs(event, sourceType, className, identifier);
	}

	private void onSlashDocs(@NotNull GuildSlashEvent event,
	                         @NotNull DocSourceType sourceType,
	                         @NotNull String className,
	                         @Nullable String identifier) throws IOException {

		final DocIndex docIndex = docIndexMap.get(sourceType);

		if (identifier == null) {
			CommonDocsHandlers.handleClass(event, className, docIndex);
		} else if (identifier.contains("(")) { //prob a method
			CommonDocsHandlers.handleMethodDocs(event, className, identifier, docIndex);
		} else {
			CommonDocsHandlers.handleFieldDocs(event, className, identifier, docIndex);
		}
	}
}
