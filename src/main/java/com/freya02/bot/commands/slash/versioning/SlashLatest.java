package com.freya02.bot.commands.slash.versioning;

import com.freya02.bot.versioning.ArtifactInfo;
import com.freya02.bot.versioning.LibraryType;
import com.freya02.bot.versioning.Versions;
import com.freya02.botcommands.api.annotations.Optional;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;

public class SlashLatest extends ApplicationCommand {
	private final Versions versions;

	public SlashLatest(Versions versions) {
		this.versions = versions;
	}

	@JDASlashCommand(
			name = "latest",
			description = "Shows the latest version of the library"
	)
	public void onSlashLatest(GuildSlashEvent event,
	                          @Optional @AppOption(description = "Type of library") LibraryType libraryType) {
		final EmbedBuilder builder = new EmbedBuilder();

		builder.setTitle("Latest versions");
		if (libraryType == null) {
			addBCVersion(builder);

			builder.addBlankField(true);

			addJDA5Version(builder);
			addJDA4Version(builder);

			builder.addBlankField(true);
		} else if (libraryType == LibraryType.BOT_COMMANDS) {
			addBCVersion(builder);
		} else if (libraryType == LibraryType.JDA5) {
			addJDA5Version(builder);
		} else if (libraryType == LibraryType.JDA4) {
			addJDA4Version(builder);
		}

		event.replyEmbeds(builder.build())
//				.setEphemeral(true)
				.queue();
	}

	private void addJDA4Version(EmbedBuilder builder) {
		final ArtifactInfo latestJDA4Version = versions.getLatestJDA4Version();

		builder.addField("JDA 4", '`' + latestJDA4Version.version() + '`', true);
	}

	private void addJDA5Version(EmbedBuilder builder) {
		final ArtifactInfo latestJDA5Version = versions.getLatestJDA5Version();

		builder.addField("JDA 5", '`' + latestJDA5Version.version() + '`', true);
	}

	private void addBCVersion(EmbedBuilder builder) {
		final ArtifactInfo latestBotCommands = versions.getLatestBotCommandsVersion();
		final ArtifactInfo jdaVersionFromBotCommands = versions.getJdaVersionFromBotCommands();

		builder.addField("BotCommands version", '`' + latestBotCommands.version() + '`', true);
		builder.addField("JDA version for BC", '`' + jdaVersionFromBotCommands.version() + '`', true);
	}
}