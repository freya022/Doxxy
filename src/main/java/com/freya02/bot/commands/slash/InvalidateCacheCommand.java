package com.freya02.bot.commands.slash;

import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.CommandPath;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.annotations.Test;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InvalidateCacheCommand extends ApplicationCommand {
	@Override
	@NotNull
	public List<Command.Choice> getOptionChoices(@Nullable Guild guild, @NotNull CommandPath commandPath, int optionIndex) {
		if (commandPath.equals(CommandPath.ofName("invalidate"))) {
			if (optionIndex == 0) {
				return List.of(
						new Command.Choice(CommonDocsHandlers.CLASS_NAME_AUTOCOMPLETE_NAME, CommonDocsHandlers.CLASS_NAME_AUTOCOMPLETE_NAME),
						new Command.Choice(CommonDocsHandlers.METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME, CommonDocsHandlers.METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME)
				);
			}
		}

		return super.getOptionChoices(guild, commandPath, optionIndex);
	}

	@Test(guildIds = 722891685755093072L)
	@JDASlashCommand(
			name = "invalidate"
	)
	public void onSlashInvalidate(GuildSlashEvent event, @AppOption String name) {
		event.getContext().invalidateAutocompletionCache(name);
	}
}
