package com.freya02.bot.commands.slash.versioning;

import com.freya02.bot.versioning.ArtifactInfo;
import com.freya02.bot.versioning.LibraryType;
import com.freya02.bot.versioning.Versions;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;

public class SlashLatest extends ApplicationCommand {
	private final Versions versions;

	public SlashLatest() {
		this.versions = new Versions();
	}

	@JDASlashCommand(
			name = "latest",
			description = "Shows the latest version of the library"
	)
	public void onSlashLatest(GuildSlashEvent event, @AppOption LibraryType libraryType) {
		if (libraryType == LibraryType.BOT_COMMANDS) {
			final ArtifactInfo latestBotCommands = versions.getLatestBotCommandsVersion();
			final ArtifactInfo jdaVersionFromBotCommands = versions.getJdaVersionFromBotCommands();

			final EmbedBuilder builder = new EmbedBuilder();
			builder.setTitle("Latest version for BotCommands");
			builder.addField("BotCommands version", '`' + latestBotCommands.version() + '`', true);
			builder.addField("JDA version for BC", '`' + jdaVersionFromBotCommands.version() + '`', true);

			event.replyEmbeds(builder.build())
//					.setEphemeral(true)
					.queue();
		} else if (libraryType == LibraryType.JDA) {
			final ArtifactInfo latestJDAVersion = versions.getLatestJDAVersion();

			final EmbedBuilder builder = new EmbedBuilder();
			builder.setTitle("Latest version for JDA");
			builder.setDescription('`' + latestJDAVersion.version() + '`');

			event.replyEmbeds(builder.build())
//					.setEphemeral(true)
					.queue();
		}
	}
}