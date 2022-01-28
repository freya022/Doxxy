package com.freya02.bot.docs;

import com.freya02.botcommands.api.BContext;
import com.freya02.botcommands.api.parameters.ParameterResolver;
import com.freya02.botcommands.api.parameters.SlashParameterResolver;
import com.freya02.botcommands.internal.application.slash.SlashCommandInfo;
import com.freya02.docs.DocSourceType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class DocSourceTypeResolver extends ParameterResolver implements SlashParameterResolver {
	public DocSourceTypeResolver() {
		super(DocSourceType.class);
	}

	@Override
	@NotNull
	public OptionType getOptionType() {
		return OptionType.STRING;
	}

	@Override
	@Nullable
	public Object resolve(@NotNull BContext context, @NotNull SlashCommandInfo info, @NotNull CommandInteractionPayload event, @NotNull OptionMapping optionMapping) {
		return DocSourceType.valueOf(optionMapping.getAsString());
	}

	@Override
	public Collection<Command.Choice> getPredefinedChoices() {
		return List.of(
				new Command.Choice("BotCommands", DocSourceType.BOT_COMMANDS.name()),
				new Command.Choice("JDA", DocSourceType.JDA.name()),
				new Command.Choice("Java", DocSourceType.JAVA.name())
		);
	}
}
