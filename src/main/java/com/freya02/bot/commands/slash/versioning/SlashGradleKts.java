package com.freya02.bot.commands.slash.versioning;

import com.freya02.bot.commands.slash.DeleteButtonListener;
import com.freya02.bot.utils.Utils;
import com.freya02.bot.versioning.LibraryType;
import com.freya02.bot.versioning.Versions;
import com.freya02.bot.versioning.supplier.BuildToolType;
import com.freya02.bot.versioning.supplier.DependencySupplier;
import com.freya02.botcommands.api.annotations.Optional;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;

public class SlashGradleKts extends ApplicationCommand {
	private final Versions versions;

	public SlashGradleKts(Versions versions) {
		this.versions = versions;
	}

	@JDASlashCommand(
			name = "gradle_kts",
			description = "Shows the Kotlin Gradle dependencies for a library"
	)
	public void onSlashGradle(GuildSlashEvent event,
	                         @Optional @AppOption(description = "Type of library") LibraryType libraryType) {

		if (libraryType == null) {
			libraryType = Utils.isBCGuild(event.getGuild())
					? LibraryType.BOT_COMMANDS
					: LibraryType.JDA5;
		}

		final EmbedBuilder builder = new EmbedBuilder();

		builder.setTitle("Kotlin Gradle dependencies for " + libraryType.getDisplayString());

		final String script = switch (libraryType) {
			case BOT_COMMANDS -> DependencySupplier.formatBC(BuildToolType.GRADLE_KTS, versions.getJdaVersionFromBotCommands(), versions.getLatestBotCommandsVersion());
			case JDA5 -> DependencySupplier.formatJDA5(BuildToolType.GRADLE_KTS, versions.getLatestJDA5Version());
			case JDA4 -> DependencySupplier.formatJDA4(BuildToolType.GRADLE_KTS, versions.getLatestJDA4Version());
		};

		builder.setDescription("```gradle\n" + script + "```");

		event.replyEmbeds(builder.build())
				.addActionRow(DeleteButtonListener.getDeleteButton(event.getUser()))
				.queue();
	}
}