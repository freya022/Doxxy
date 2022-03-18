package com.freya02.bot;

import com.freya02.botcommands.api.CommandList;
import com.freya02.botcommands.api.SettingsProvider;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;

public class DoxxySettings implements SettingsProvider {
	@Override
	@NotNull
	public CommandList getGuildCommands(@NotNull Guild guild) {
		//If this is not a BC guild then we need to disable BC related subcommands in these guilds
		if (guild.getIdLong() != 722891685755093072L && guild.getIdLong() != 848502702731165738L) {
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
