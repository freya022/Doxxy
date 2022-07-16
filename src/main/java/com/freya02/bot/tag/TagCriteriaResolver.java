package com.freya02.bot.tag;

import com.freya02.botcommands.api.BContext;
import com.freya02.botcommands.api.parameters.ParameterResolver;
import com.freya02.botcommands.api.parameters.SlashParameterResolver;
import com.freya02.botcommands.internal.application.slash.SlashCommandInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class TagCriteriaResolver extends ParameterResolver implements SlashParameterResolver {
	public TagCriteriaResolver() {
		super(TagCriteria.class);
	}

	@Override
	@NotNull
	public OptionType getOptionType() {
		return OptionType.STRING;
	}

	@Override
	@Nullable
	public Object resolve(@NotNull BContext context, @NotNull SlashCommandInfo info, @NotNull CommandInteractionPayload event, @NotNull OptionMapping optionMapping) {
		return TagCriteria.valueOf(optionMapping.getAsString());
	}

	@Override
	@NotNull
	public Collection<Command.Choice> getPredefinedChoices(@Nullable Guild guild) {
		return List.of(
				new Command.Choice("Name", TagCriteria.NAME.name()),
				new Command.Choice("Uses", TagCriteria.USES.name())
		);
	}
}
