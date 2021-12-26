package com.freya02.bot.docs;

import com.freya02.botcommands.api.parameters.ParameterResolver;
import com.freya02.botcommands.api.parameters.SlashParameterResolver;
import com.freya02.docs.DocSourceType;
import net.dv8tion.jda.api.interactions.commands.CommandPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.Nullable;

public class DocSourceTypeResolver extends ParameterResolver implements SlashParameterResolver {
	public DocSourceTypeResolver() {
		super(DocSourceType.class);
	}

	@Override
	@Nullable
	public Object resolve(CommandPayload event, OptionMapping optionMapping) {
		return DocSourceType.valueOf(optionMapping.getAsString());
	}
}
