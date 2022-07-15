package com.freya02.bot.commands.text;

import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.annotations.Test;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;

public class SlashInvite extends ApplicationCommand {
	@Test
	@JDASlashCommand(
			name = "invite"
	)
	public void onSlashInvite(GuildSlashEvent event) {
		event.reply(event.getJDA().getInviteUrl()).queue();
	}
}
