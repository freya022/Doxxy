package com.freya02.bot.commands.slash.docs;

import com.freya02.bot.docs.DocIndexMap;
import com.freya02.bot.docs.index.DocIndex;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.docs.DocSourceType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class AnyMethodCommand extends ApplicationCommand {
	private final DocIndexMap docIndexMap;

	public AnyMethodCommand() throws IOException {
		docIndexMap = DocIndexMap.getInstance();
	}

	@JDASlashCommand(
			name = "anymethod",
			description = "Shows the documentation for any method"
	)
	public void onSlashAnyMethod(@NotNull GuildSlashEvent event,
	                             @NotNull @AppOption(description = "The docs to search upon")
			                             DocSourceType sourceType,
	                             @NotNull @AppOption(description = "Full signature of the class + method", autocomplete = CommonDocsHandlers.ANY_METHOD_NAME_AUTOCOMPLETE_NAME)
			                             String fullSignature) throws IOException {

		final DocIndex docIndex = docIndexMap.get(sourceType);

		final MessageEmbed methodDoc = docIndex.getMethodDoc(fullSignature);

		if (methodDoc == null) {
			event.reply("Unknown method").setEphemeral(true).queue();

			return;
		}

		CommonDocsHandlers.sendMethod(event, false, methodDoc);
	}
}