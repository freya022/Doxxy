package com.freya02.bot;

import com.freya02.bot.docs.BasicDocs;
import com.freya02.bot.docs.ClassList;
import com.freya02.bot.docs.Detail;

import java.io.IOException;

public class DocsTest {
	public static void main(String[] args) throws IOException {
		final String url = "https://ci.dv8tion.net/job/JDA/javadoc/net/dv8tion/jda/api/entities/Guild.BoostTier.html";
//		final String url = "https://docs.oracle.com/javase/9/docs/api/java/lang/StringBuilder.html";

		final BasicDocs docs = new BasicDocs(url);
		System.out.println(docs.getClassDecl().getMarkdown2(false));
		System.out.println(docs.getDescription().getMarkdown());
		for (Detail detail : docs.getDetails()) {
			System.out.println(detail.key().getMarkdown());
			System.out.println(detail.value().getMarkdown());
		}

		ClassList.runGeneration("https://ci.dv8tion.net/job/JDA/javadoc/allclasses.html");

		System.out.println();

//		final Matcher matcher = CLASS_DESC_PATTERN.matcher(input);
//
//		while (matcher.find()) {
//			final String group = parse(matcher.group(3), url);
//
//			System.out.println(group);
//		}
	}
}