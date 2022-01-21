package com.freya02.bot.commands.slash.docs;

import com.freya02.bot.docs.DocIndexMap;
import com.freya02.bot.docs.index.DocIndex;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import com.freya02.docs.DocSourceType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class MethodCommand extends ApplicationCommand {
	private final DocIndexMap docIndexMap;

	public MethodCommand() throws IOException {
		docIndexMap = DocIndexMap.getInstance();
	}

	@JDASlashCommand(
			name = "method",
			description = "Shows the documentation for a method of a class"
	)
	public void onSlashMethod(@NotNull GuildSlashEvent event,
	                          @NotNull @AppOption(description = "The docs to search upon")
			                          DocSourceType sourceType,
	                          @NotNull @AppOption(description = "Name of the Java class", autocomplete = CommonDocsHandlers.CLASS_NAME_WITH_METHODS_AUTOCOMPLETE_NAME)
			                          String className,
	                          @NotNull @AppOption(description = "Signature of the method", autocomplete = CommonDocsHandlers.METHOD_NAME_BY_CLASS_AUTOCOMPLETE_NAME)
			                          String methodId) throws IOException {

		final DocIndex docIndex = docIndexMap.get(sourceType);
		final MessageEmbed classDoc = docIndex.getClassDoc(className);

		if (classDoc == null) {
			event.reply("Unknown class").setEphemeral(true).queue();

			return;
		}

		final MessageEmbed methodDoc = docIndex.getMethodDoc(className, methodId);

		if (methodDoc == null) {
			event.reply("Unknown method").setEphemeral(true).queue();

			return;
		}

		CommonDocsHandlers.sendMethod(event, false, methodDoc);
	}
}
