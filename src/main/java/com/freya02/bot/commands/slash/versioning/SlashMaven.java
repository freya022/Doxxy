package com.freya02.bot.commands.slash.versioning;

import com.freya02.bot.versioning.ArtifactInfo;
import com.freya02.bot.versioning.LibraryType;
import com.freya02.bot.versioning.Versions;
import com.freya02.botcommands.api.application.ApplicationCommand;
import com.freya02.botcommands.api.application.annotations.AppOption;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.botcommands.api.application.slash.annotations.JDASlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import org.intellij.lang.annotations.Language;

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
	                         @AppOption(description = "Type of library") LibraryType libraryType) {
		final EmbedBuilder builder = new EmbedBuilder();

		if (libraryType == LibraryType.BOT_COMMANDS) {
			final ArtifactInfo latestBotCommands = versions.getLatestBotCommandsVersion();
			final ArtifactInfo jdaVersionFromBotCommands = versions.getJdaVersionFromBotCommands();

			builder.setTitle("Maven dependencies for BotCommands");
			@Language("xml")
			final String xml = """
					<repositories>
					    <repository>
					        <id>dv8tion</id>
					        <name>m2-dv8tion</name>
					        <url>https://m2.dv8tion.net/releases</url>
					    </repository>
					    <repository>
					        <id>jitpack</id>
					        <url>https://jitpack.io</url>
					    </repository>
					</repositories>
					
					...
					
					<dependencies>
						<dependency>
							<groupId>%s</groupId>
							<artifactId>%s</artifactId>
							<version>%s</version>
						</dependency>
						<dependency>
							<groupId>%s</groupId>
							<artifactId>%s</artifactId>
							<version>%s</version>
						</dependency>
					</dependencies>
					""".formatted(jdaVersionFromBotCommands.groupId(), jdaVersionFromBotCommands.artifactId(), jdaVersionFromBotCommands.version(),
					latestBotCommands.groupId(), latestBotCommands.artifactId(), latestBotCommands.version());

			builder.setDescription("```xml\n" + xml + "```");

			event.replyEmbeds(builder.build())
//					.setEphemeral(true)
					.queue();
		} else if (libraryType == LibraryType.JDA) {
			final ArtifactInfo latestJDAVersion = versions.getLatestJDAVersion();

			builder.setTitle("Maven dependencies for JDA");

			@Language("xml")
			final String xml = """
					<repositories>
					    <repository>
					        <id>m2-dv8tion</id>
					        <name>m2-dv8tion</name>
					        <url>https://m2.dv8tion.net/releases</url>
					    </repository>
					</repositories>
					
					...
					
					<dependencies>
						<dependency>
							<groupId>%s</groupId>
							<artifactId>%s</artifactId>
							<version>%s</version>
						</dependency>
					</dependencies>
					""".formatted(latestJDAVersion.groupId(), latestJDAVersion.artifactId(), latestJDAVersion.version());

			builder.setDescription("```xml\n" + xml + "```");

			event.replyEmbeds(builder.build())
//					.setEphemeral(true)
					.queue();
		}
	}
}