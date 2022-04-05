package com.freya02.bot.commands.slash.docs;

import com.freya02.bot.docs.index.DocIndex;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.docs.DocSourceType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class AnyMethodCommand extends BaseDocCommand {
	public AnyMethodCommand() throws IOException {}

	@JDASlashCommand(
			name = "anymethod",
			subcommand = "botcommands",
			description = "Shows the documentation for any method"
	)
	public void onSlashAnyMethodBC(@NotNull GuildSlashEvent event,
	                               @NotNull @AppOption(description = "The docs to search upon")
			                               DocSourceType sourceType,
	                               @NotNull @AppOption(description = "Full signature of the class + method", autocomplete = CommonDocsHandlers.ANY_METHOD_NAME_AUTOCOMPLETE_NAME)
			                               String fullSignature) throws IOException {
		onSlashAnyMethod(event, sourceType, fullSignature);
	}

	@JDASlashCommand(
			name = "anymethod",
			subcommand = "jda",
			description = "Shows the documentation for any method"
	)
	public void onSlashAnyMethodJDA(@NotNull GuildSlashEvent event,
	                                @NotNull @AppOption(description = "The docs to search upon")
			                                DocSourceType sourceType,
	                                @NotNull @AppOption(description = "Full signature of the class + method", autocomplete = CommonDocsHandlers.ANY_METHOD_NAME_AUTOCOMPLETE_NAME)
			                                String fullSignature) throws IOException {
		onSlashAnyMethod(event, sourceType, fullSignature);
	}

	@JDASlashCommand(
			name = "anymethod",
			subcommand = "java",
			description = "Shows the documentation for any method"
	)
	public void onSlashAnyMethodJava(@NotNull GuildSlashEvent event,
	                                 @NotNull @AppOption(description = "The docs to search upon")
			                                 DocSourceType sourceType,
	                                 @NotNull @AppOption(description = "Full signature of the class + method", autocomplete = CommonDocsHandlers.ANY_METHOD_NAME_AUTOCOMPLETE_NAME)
			                                 String fullSignature) throws IOException {
		onSlashAnyMethod(event, sourceType, fullSignature);
	}

	private void onSlashAnyMethod(@NotNull GuildSlashEvent event,
	                              @NotNull DocSourceType sourceType,
	                              @NotNull String fullSignature) throws IOException {

		final String[] split = fullSignature.split("#");
		if (split.length != 2) {
			event.reply("You must supply a class name and a method signature, separated with a #, e.g. `Object#getClass()`").setEphemeral(true).queue();

			return;
		}

		final DocIndex docIndex = docIndexMap.get(sourceType);

		CommonDocsHandlers.handleMethodDocs(event, split[0], split[1], docIndex);
	}
}
