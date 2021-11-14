package com.freya02.bot.commands;

import com.freya02.bot.docs.BasicDocs;
import com.freya02.bot.docs.ClassList;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.AutocompletionHandler;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import net.dv8tion.jda.api.events.interaction.CommandAutoCompleteEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ClassCommand extends ApplicationCommand {
	private static final String BASE_URL = "https://ci.dv8tion.net/job/JDA/javadoc/";
	private final Map<String, String> classes;
	private final List<String> classList;

	public ClassCommand() throws IOException {
		ClassList classes = ClassList.of(BASE_URL + "allclasses.html");

		this.classes = classes.getClassToUrlMap();
		this.classList = new ArrayList<>(this.classes.keySet());
		classList.sort(Comparator.naturalOrder());
	}

	@JDASlashCommand(name = "class")
	public void showClass(GuildSlashEvent event, @AppOption(description = "Name of the Java class", autocomplete = "autoClass") String className) throws IOException {
		final String relativeUrl = classes.get(className);

		if (relativeUrl == null) {
			event.reply("Unknown class").setEphemeral(true).queue();

			return;
		}

		final String url = BASE_URL + relativeUrl;

		event.deferReply(true).queue();

		final BasicDocs docs = BasicDocs.of(url);

		event.getHook().sendMessageEmbeds(docs.toEmbed().build()).queue();
	}

	@AutocompletionHandler(name = "autoClass")
	public List<String> autoClass(CommandAutoCompleteEvent event) {
		return classList;
	}
}
