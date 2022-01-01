package com.freya02.bot;

import com.freya02.bot.utils.HttpUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class DocsTest2 {
	public static void main(String[] args) throws Exception {
		final Document document = HttpUtils.getDocument("https://ci.dv8tion.net/job/JDA/javadoc/allclasses.html");

		final Path input = Main.BOT_FOLDER.resolve("docs_cache\\ci.dv8tion.net\\job\\JDA\\javadoc\\constant-values.html");
		final Path output = Main.BOT_FOLDER.resolve("docs_cache\\ci.dv8tion.net\\job\\JDA\\javadoc\\test.html");

		final String html = Files.readString(input);
		final String clean = Jsoup.clean(html, "https://ci.dv8tion.net/job/JDA/javadoc/allclasses.html", Safelist.relaxed());

		Files.writeString(output, clean, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		final Parser parser = Parser.htmlParser();
		TestUtils.measureTime("dirty doc", 1000, 1000, () -> {
			final Document dirtyDoc = Jsoup.parse(html, "https://ci.dv8tion.net/job/JDA/javadoc/allclasses.html", parser);
		});

		TestUtils.measureTime("clean doc", 1000, 1000, () -> {
			final Document cleanDoc = Jsoup.parse(clean, "https://ci.dv8tion.net/job/JDA/javadoc/allclasses.html", parser);
		});

		System.out.println();
	}
}
