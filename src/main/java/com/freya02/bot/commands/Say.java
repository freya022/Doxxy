package com.freya02.bot.commands;

import com.freya02.botcommands.api.annotations.RequireOwner;
import com.freya02.botcommands.api.prefixed.BaseCommandEvent;
import com.freya02.botcommands.api.prefixed.TextCommand;
import com.freya02.botcommands.api.prefixed.annotations.JDATextCommand;
import com.freya02.botcommands.api.prefixed.annotations.TextOption;
import net.dv8tion.jda.api.EmbedBuilder;

public class Say extends TextCommand {
	@RequireOwner
	@JDATextCommand(name = "say")
	public void say(BaseCommandEvent event, @TextOption String str) {
		event.respond(new EmbedBuilder().setDescription(str).build()).queue();
	}
}
