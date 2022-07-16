package com.freya02.bot;

import com.freya02.bot.utils.Utils;
import com.freya02.botcommands.api.CommandList;
import com.freya02.botcommands.api.SettingsProvider;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;

public class BotSettings implements SettingsProvider {
	@Override
	@NotNull
	public CommandList getGuildCommands(@NotNull Guild guild) {
		//If this is not a BC guild then we need to disable BC related subcommands in these guilds
		if (!Utils.isBCGuild(guild)) {
			return CommandList.filter(commandPath -> {
				if (commandPath.getSubname() != null) {
					return !commandPath.getSubname().equals("botcommands");
				}

				return true;
			});
		}

		return SettingsProvider.super.getGuildCommands(guild);
	}
}
