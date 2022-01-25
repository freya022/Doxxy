package com.freya02.bot.commands.slash.docs;

import com.freya02.bot.docs.DocIndexMap;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.annotations.Test;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.docs.DocSourceType;

import java.io.IOException;

import static com.freya02.bot.commands.slash.docs.CommonDocsHandlers.AUTOCOMPLETE_NAMES;

public class InvalidateCacheCommand extends ApplicationCommand {
	@Test
	@JDASlashCommand(
			name = "invalidate"
	)
	public void onSlashInvalidate(GuildSlashEvent event) throws IOException {
		event.deferReply().queue();

		DocIndexMap.refreshAndInvalidateIndex(DocSourceType.BOT_COMMANDS);
		DocIndexMap.refreshAndInvalidateIndex(DocSourceType.JDA);

		for (String autocompleteName : AUTOCOMPLETE_NAMES) {
			event.getContext().invalidateAutocompletionCache(autocompleteName);
		}

		event.reply("Done").setEphemeral(true).queue();
	}
}
