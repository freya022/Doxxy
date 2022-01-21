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

public class AnyFieldCommand extends ApplicationCommand {
	private final DocIndexMap docIndexMap;

	public AnyFieldCommand() throws IOException {
		docIndexMap = DocIndexMap.getInstance();
	}

	@JDASlashCommand(
			name = "anyfield",
			description = "Shows the documentation for any field"
	)
	public void onSlashAnyField(@NotNull GuildSlashEvent event,
	                            @NotNull @AppOption(description = "The docs to search upon")
			                            DocSourceType sourceType,
	                            @NotNull @AppOption(description = "Full signature of the class + field", autocomplete = CommonDocsHandlers.ANY_FIELD_NAME_AUTOCOMPLETE_NAME)
			                            String fullSignature) throws IOException {

		final DocIndex docIndex = docIndexMap.get(sourceType);

		final MessageEmbed fieldDoc = docIndex.getFieldDoc(fullSignature);

		if (fieldDoc == null) {
			event.reply("Unknown field").setEphemeral(true).queue();

			return;
		}

		CommonDocsHandlers.sendField(event, false, fieldDoc);
	}
}
