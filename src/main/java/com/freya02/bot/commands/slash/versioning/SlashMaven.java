package com.freya02.bot.commands.slash.versioning;

import com.freya02.bot.commands.slash.DeleteButtonListener;
import com.freya02.bot.versioning.ArtifactInfo;
import com.freya02.bot.versioning.LibraryType;
import com.freya02.bot.versioning.Versions;
import com.freya02.botcommands.api.annotations.Optional;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;

public class SlashMaven extends ApplicationCommand {
	private final Versions versions;

	public SlashMaven(Versions versions) {
		this.versions = versions;
	}

	@JDASlashCommand(
			name = "maven",
			description = "Shows the maven dependencies for a library"
	)
	public void onSlashMaven(GuildSlashEvent event,
	                         @Optional @AppOption(description = "Type of library") LibraryType libraryType) {
		final EmbedBuilder builder = new EmbedBuilder();

		final String xml;
		if (libraryType == null || libraryType == LibraryType.BOT_COMMANDS) { //Default
			final ArtifactInfo latestBotCommands = versions.getLatestBotCommandsVersion();
			final ArtifactInfo jdaVersionFromBotCommands = versions.getJdaVersionFromBotCommands();

			builder.setTitle("Maven dependencies for BotCommands");
			xml = VersioningCommons.formatBC(jdaVersionFromBotCommands, latestBotCommands);
		} else if (libraryType == LibraryType.JDA5) {
			final ArtifactInfo latestJDAVersion = versions.getLatestJDA5Version();

			builder.setTitle("Maven dependencies for JDA 5");

			xml = VersioningCommons.formatJDA5(latestJDAVersion);
		} else if (libraryType == LibraryType.JDA4) {
			final ArtifactInfo latestJDAVersion = versions.getLatestJDA4Version();

			builder.setTitle("Maven dependencies for JDA 4");

			xml = VersioningCommons.formatJDA4(latestJDAVersion);
		} else {
			throw new IllegalArgumentException();
		}

		builder.setDescription("```xml\n" + xml + "```");

		event.replyEmbeds(builder.build())
//				.setEphemeral(true)
				.addActionRow(DeleteButtonListener.getDeleteButton(event.getUser()))
				.queue();
	}
}