package com.freya02.bot;

import com.freya02.bot.db.Database;
import com.freya02.bot.docs.DocSourceTypeResolver;
import com.freya02.bot.tag.TagCriteriaResolver;
import com.freya02.bot.utils.FileCache;
import com.freya02.botcommands.api.CommandsBuilder;
import com.freya02.botcommands.api.Logging;
import com.freya02.botcommands.api.components.DefaultComponentManager;
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
	public static final Path RENDERED_DOCS_CACHE_PATH;

	private static final Logger LOGGER = Logging.getLogger();

	static {
		try {
			if (Files.notExists(BOT_FOLDER)) {
				throw new IllegalStateException("Bot folder at " + BOT_FOLDER + " does not exist !");
			}

			Files.createDirectories(CACHE_PATH);

			final FileCache renderedDocsCache = new FileCache(BOT_FOLDER, "rendered_docs", true);
			RENDERED_DOCS_CACHE_PATH = renderedDocsCache.getCachePath();

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					renderedDocsCache.close();
				} catch (IOException e) {
					LOGGER.error("Unable to close cache", e);
				}
			}));
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

			CommandsBuilder.newBuilder(222046562543468545L)
					.extensionsBuilder(extensionsBuilder -> extensionsBuilder
							.registerConstructorParameter(Config.class, commandType -> config)
							.registerConstructorParameter(Database.class, commandType -> database)
							.registerParameterResolver(new TagCriteriaResolver())
							.registerParameterResolver(new DocSourceTypeResolver())
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
