package com.freya02.bot.tag;

import com.freya02.botcommands.api.parameters.ParameterResolver;
import com.freya02.botcommands.api.parameters.SlashParameterResolver;
import net.dv8tion.jda.api.events.interaction.commands.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

public class TagCriteriaResolver extends ParameterResolver implements SlashParameterResolver {
	public TagCriteriaResolver() {
		super(TagCriteria.class);
	}

	@Override
	@NotNull
	public Object resolve(SlashCommandEvent event, OptionMapping optionMapping) {
		return TagCriteria.valueOf(optionMapping.getAsString());
	}
}
