package com.freya02.bot.commands.slash.docs;

import com.freya02.bot.docs.DocIndexMap;
import com.freya02.botcommands.api.BContext;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.CommandPath;
import com.freya02.botcommands.api.application.slash.DefaultValueSupplier;
import com.freya02.docs.DocSourceType;
import net.dv8tion.jda.api.entities.Guild;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public abstract class BaseDocCommand extends ApplicationCommand {
	protected final DocIndexMap docIndexMap;

	public BaseDocCommand() throws IOException {
		docIndexMap = DocIndexMap.getInstance();
	}

	@Override
	@Nullable
	public DefaultValueSupplier getDefaultValueSupplier(@NotNull BContext context, @NotNull Guild guild,
	                                                    @Nullable String commandId, @NotNull CommandPath commandPath,
	                                                    @NotNull String optionName, @NotNull Class<?> parameterType) {
		if (optionName.equals("source_type")) {
			//use subcommand as a default value
			if ("botcommands".equals(commandPath.getSubname())) {
				return e -> DocSourceType.BOT_COMMANDS;
			} else if ("jda".equals(commandPath.getSubname())) {
				return e -> DocSourceType.JDA;
			} else if ("java".equals(commandPath.getSubname())) {
				return e -> DocSourceType.JAVA;
			}
		}

		return super.getDefaultValueSupplier(context, guild, commandId, commandPath, optionName, parameterType);
	}
}
