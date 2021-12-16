package com.freya02.bot;

import com.freya02.botcommands.api.CommandsBuilder;
import com.freya02.botcommands.api.components.DefaultComponentManager;
import com.freya02.botcommands.internal.Logging;
import com.freya02.docs.ClassDocs;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
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
			ClassDocs.loadAllDocs("http://localhost:63342/DocsBot/test_docs/allclasses-index.html");

			LOGGER.info("Docs loaded");

			final JDA jda = JDABuilder.createLight(config.getToken())
					.setActivity(Activity.watching("the docs"))
					.build()
					.awaitReady();

			LOGGER.info("Loaded JDA");

			final Database database = new Database(config);

			CommandsBuilder.newBuilder(222046562543468545L)
					.extensionsBuilder(extensionsBuilder -> extensionsBuilder
							.registerConstructorParameter(Config.class, commandType -> config)
					)
					.textCommandBuilder(textCommandsBuilder -> textCommandsBuilder
							.addPrefix("!")
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
