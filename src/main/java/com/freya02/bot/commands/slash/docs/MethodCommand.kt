package com.freya02.bot.commands.slash.docs;

import com.freya02.bot.docs.index.DocIndex;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.docs.DocSourceType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class MethodCommand extends BaseDocCommand {
	public MethodCommand() throws IOException {}

	@JDASlashCommand(
			name = "method",
			subcommand = "botcommands",
			description = "Shows the documentation for a method of a class"
	)
	public void onSlashMethodBC(@NotNull GuildSlashEvent event,
	                            @NotNull @AppOption(description = "The docs to search upon")
			                            DocSourceType sourceType,
	                            @NotNull @AppOption(description = "Name of the Java class", autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME)
			                            String className,
	                            @NotNull @AppOption(description = "Signature of the method", autocomplete = CommonDocsHandlers.METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME)
			                            String methodId) throws IOException {
		onSlashMethod(event, sourceType, className, methodId);
	}

	@JDASlashCommand(
			name = "method",
			subcommand = "jda",
			description = "Shows the documentation for a method of a class"
	)
	public void onSlashMethodJDA(@NotNull GuildSlashEvent event,
	                             @NotNull @AppOption(description = "The docs to search upon")
			                             DocSourceType sourceType,
	                             @NotNull @AppOption(description = "Name of the Java class", autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME)
			                             String className,
	                             @NotNull @AppOption(description = "Signature of the method", autocomplete = CommonDocsHandlers.METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME)
			                             String methodId) throws IOException {
		onSlashMethod(event, sourceType, className, methodId);
	}

	@JDASlashCommand(
			name = "method",
			subcommand = "java",
			description = "Shows the documentation for a method of a class"
	)
	public void onSlashMethodJava(@NotNull GuildSlashEvent event,
	                              @NotNull @AppOption(description = "The docs to search upon")
			                              DocSourceType sourceType,
	                              @NotNull @AppOption(description = "Name of the Java class", autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME)
			                              String className,
	                              @NotNull @AppOption(description = "Signature of the method", autocomplete = CommonDocsHandlers.METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME)
			                              String methodId) throws IOException {
		onSlashMethod(event, sourceType, className, methodId);
	}

	private void onSlashMethod(@NotNull GuildSlashEvent event,
	                           @NotNull DocSourceType sourceType,
	                           @NotNull String className,
	                           @NotNull String methodId) throws IOException {

		final DocIndex docIndex = docIndexMap.get(sourceType);

		CommonDocsHandlers.handleMethodDocs(event, className, methodId, docIndex);
	}
}
