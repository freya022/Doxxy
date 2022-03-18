package com.freya02.bot;

import com.freya02.bot.db.Database;
import com.freya02.bot.docs.DocSourceTypeResolver;
import com.freya02.bot.tag.TagCriteriaResolver;
import com.freya02.bot.versioning.LibraryTypeResolver;
import com.freya02.bot.versioning.Versions;
import com.freya02.botcommands.api.CommandsBuilder;
import com.freya02.botcommands.api.Logging;
import com.freya02.botcommands.api.components.DefaultComponentManager;
import com.freya02.docs.DocWebServer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
	public static final Path BOT_FOLDER = Path.of(System.getProperty("user.home"), "Downloads", "DocsBot");
	public static final Path CACHE_PATH = BOT_FOLDER.resolve("docs_cache");
	public static final Path JAVADOCS_PATH = BOT_FOLDER.resolve("javadocs");
	public static final Path REPOS_PATH = BOT_FOLDER.resolve("repos");
	public static final Path LAST_KNOWN_VERSIONS_FOLDER_PATH = BOT_FOLDER.resolve("last_versions");

	private static final Logger LOGGER = Logging.getLogger();

	static {
		try {
			if (Files.notExists(BOT_FOLDER)) {
				throw new IllegalStateException("Bot folder at " + BOT_FOLDER + " does not exist !");
			}

			Files.createDirectories(CACHE_PATH);
			Files.createDirectories(REPOS_PATH);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		try {
			final Config config = Config.getConfig();

			final JDA jda = JDABuilder.createLight(config.getToken())
					.setActivity(Activity.watching("the docs"))
					.build()
					.awaitReady();

			LOGGER.info("Loaded JDA");

			final Database database = new Database(config);

			LOGGER.info("Starting docs web server");
			DocWebServer.startDocWebServer();
			LOGGER.info("Started docs web server");

			final Versions versions = new Versions();

			final CommandsBuilder commandsBuilder = CommandsBuilder.newBuilder(222046562543468545L);

			commandsBuilder
					.extensionsBuilder(extensionsBuilder -> extensionsBuilder
							.registerConstructorParameter(Config.class, commandType -> config)
							.registerConstructorParameter(Database.class, commandType -> database)
							.registerParameterResolver(new TagCriteriaResolver())
							.registerParameterResolver(new DocSourceTypeResolver())
							.registerConstructorParameter(Versions.class, commandType -> versions)
							.registerParameterResolver(new LibraryTypeResolver())
					)
					.textCommandBuilder(textCommandsBuilder -> textCommandsBuilder
							.addPrefix("!")
					)
					.applicationCommandBuilder(applicationCommandsBuilder -> applicationCommandsBuilder
							.addTestGuilds(722891685755093072L)
					)
					.setComponentManager(new DefaultComponentManager(database::getConnection))
					.setSettingsProvider(new DoxxySettings())
					.build(jda, "com.freya02.bot.commands");

			versions.initUpdateLoop(commandsBuilder.getContext());

			LOGGER.info("Loaded commands");
		} catch (Exception e) {
			LOGGER.error("Unable to start the bot", e);

			System.exit(-1);
		}
	}
}
