package com.freya02.bot;

import com.freya02.bot.docs.DocIndex;
import com.freya02.docs.DocSourceType;

import java.io.IOException;

public class DocsTest3 {
	public static void main(String[] args) throws IOException {
//		TestUtils.measureTime("get all docs", 10, 10, DocsTest3::getAllDocs);

//		ClassDocs.loadAllDocs("http://localhost:63342/DocsBot/BotCommands_docs/allclasses-index.html");

//		final ClassDoc docs1 = ClassDocs.of("https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/zip/ZipEntry.html");
//		final ClassDoc docs2 = ClassDocs.of("https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/AbstractCollection.html");
//		final ClassDoc docs3 = ClassDocs.of("https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/TextField.html");

//		System.out.println(docs2.getDescriptionElement().getMarkdown());
//
//		final String html = Utils.getDocument("https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/AbstractCollection.html").outerHtml();
//		final String s = html.replaceAll("</(.*)>(\\s+)<(\\1*)>", "</$1>$2<dummy></dummy><$3>");
//
//		final Document document = Jsoup.parse(s);
//
//		final HTMLElement element = new HTMLElement(document.selectFirst("body > div.flex-box > div > main > section.description > div.block"));

//		final var e = new HTMLElement(Jsoup.parseBodyFragment("<a href=\"bruh.com\"><code>lol</code></a>", "https://lol.com"));
//		System.out.println(e.getMarkdown2(false));

//		ClassDocs.loadAllDocs("http://localhost:63342/DocsBot/BotCommands_docs/allclasses-index.html");

//		final ClassDoc doc = ClassDocs.globalCompute("http://localhost:63342/DocsBot/BotCommands_docs/com/freya02/botcommands/api/application/slash/annotations/JDASlashCommand.html#annotation-interface-element-detail");

//		final String cached = HttpUtils.downloadBodyIfNotCached("http://localhost:63342/DocsBot/BotCommands_docs/com/freya02/botcommands/api/application/slash/annotations/JDASlashCommand.html#annotation-interface-element-detail");

//		final ClassDocs bcDocs = ClassDocs.indexAll(DocSourceType.BOT_COMMANDS);
//		final ClassDocs jdaDocs = ClassDocs.indexAll(DocSourceType.JDA);
//
//		for (String value : bcDocs.getSimpleNameToUrlMap().values()) {
//			final ClassDoc doc = new ClassDoc(value);
//		}
//
//		for (String value : jdaDocs.getSimpleNameToUrlMap().values()) {
//			final ClassDoc doc = new ClassDoc(value);
//		}

//		final ClassDoc doc = classDocs.tryRetrieveDoc("AbstractComponentBuilder", true);

//		final ClassDoc doc = new ClassDoc("http://localhost:63342/DocsBot/BotCommands_docs/com/freya02/botcommands/api/components/builder/LambdaComponentBuilder.html");
//		final ClassDoc doc2 = new ClassDoc("http://localhost:63342/DocsBot/BotCommands_docs/com/freya02/botcommands/api/components/builder/AbstractLambdaComponentBuilder.html");
//		final ClassDoc doc3 = new ClassDoc("http://localhost:63342/DocsBot/BotCommands_docs/com/freya02/botcommands/api/application/annotations/AppOption.html#description()");
		final DocIndex bcIndex = new DocIndex(DocSourceType.BOT_COMMANDS);
		final DocIndex jdaIndex = new DocIndex(DocSourceType.JDA);

//		final ClassDoc doc4 = new ClassDoc("http://localhost:63342/DocsBot/BotCommands_docs/com/freya02/botcommands/api/pagination/menu/BasicMenuBuilder.html");

		System.out.println();

//
//		for (ClassDoc doc : ClassDocs.getSource(DocSourceType.BOT_COMMANDS).getDocNamesMap().values()) {
//			final List<Command.Choice> choices = doc.getMethodDocs().values().stream().map(m -> {
//				final StringBuilder simpleSignatureBuilder = new StringBuilder();
//
//				final String id = m.getElementId();
//
//				final int index = id.indexOf('(');
//				simpleSignatureBuilder.append(id, 0, index);
//
//				final StringJoiner parameterJoiner = new StringJoiner(", ", "(", ")");
//				final String[] parameters = id.substring(index + 1, id.length() - 1).split(",");
//				for (String parameter : parameters) {
//					if (parameter.isBlank()) continue;
//
//					final String className = DecomposedName.getSimpleClassName(parameter.trim());
//
//					parameterJoiner.add(className);
//				}
//
//				simpleSignatureBuilder.append(parameterJoiner);
//
//				return new Command.Choice(simpleSignatureBuilder.toString(), id);
//			}).toList();
//
//			System.out.println();
//		}


//		final ClassDoc doc4 = ClassDocs.globalCompute("https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/CopyOnWriteArraySet.html");
//		System.out.println(doc4.getDescriptionElement().getMarkdown());
	}
}
