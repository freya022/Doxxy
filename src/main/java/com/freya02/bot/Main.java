package com.freya02.bot;

import com.freya02.bot.docs.DocsCollection;
import com.freya02.botcommands.api.CommandsBuilder;
import com.freya02.botcommands.api.components.DefaultComponentManager;
import com.freya02.botcommands.internal.Logging;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
	public static final Path BOT_FOLDER = Path.of(System.getProperty("user.home"), "Downloads", "DocsBot");
	private static final Logger LOGGER = Logging.getLogger();

	static {
		if (Files.notExists(BOT_FOLDER)) {
			throw new IllegalStateException("Bot folder at " + BOT_FOLDER + " does not exist !");
		}
	}

	public static void main(String[] args) {
		try {
			final Config config = Config.getConfig();

			LOGGER.info("Loading docs");
			final DocsCollection docsCollection = DocsCollection.of("https://ci.dv8tion.net/job/JDA/javadoc/allclasses.html",
					"https://ci.dv8tion.net/job/JDA/javadoc/constant-values.html");

			LOGGER.info("Docs loaded");

			final JDA jda = JDABuilder.createLight(config.getToken())
					.build()
					.awaitReady();

			LOGGER.info("Loaded JDA");

			final Database database = new Database(config);

			CommandsBuilder.newBuilder(222046562543468545L)
					.extensionsBuilder(extensionsBuilder -> extensionsBuilder
							.registerConstructorParameter(Config.class, commandType -> config)
							.registerConstructorParameter(DocsCollection.class, commandType -> docsCollection)
					)
					.setComponentManager(new DefaultComponentManager(database::getConnection))
					.build(jda, "com.freya02.bot.commands");

			LOGGER.info("Loaded commands");
		} catch (Exception e) {
			LOGGER.error("Unable to start the bot", e);

			System.exit(-1);
		}
	}
}
