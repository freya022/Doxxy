package com.freya02.bot;

import com.freya02.bot.utils.Utils;
import com.freya02.docs.ClassDoc;
import com.freya02.docs.ClassDocs;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DocsTest3 {
	public static void main(String[] args) throws IOException {
//		TestUtils.measureTime("get all docs", 10, 10, DocsTest3::getAllDocs);

//		getAllDocs();

//		final ClassDocs docs1 = new ClassDocs("https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/zip/ZipEntry.html");
		final ClassDoc docs2 = ClassDocs.of("https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/AbstractCollection.html");
		final ClassDoc docs3 = ClassDocs.of("https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/TextField.html");

//		System.out.println(docs2.getDescriptionElement().getMarkdown3());
//
//		final String html = Utils.getDocument("https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/AbstractCollection.html").outerHtml();
//		final String s = html.replaceAll("</(.*)>(\\s+)<(\\1*)>", "</$1>$2<dummy></dummy><$3>");
//
//		final Document document = Jsoup.parse(s);
//
//		final HTMLElement element = new HTMLElement(document.selectFirst("body > div.flex-box > div > main > section.description > div.block"));

		System.out.println();
	}

	private static void getAllDocs() {
		try {
			final Document document = Utils.getDocument("https://ci.dv8tion.net/job/JDA/javadoc/allclasses.html");

			final Map<String, ClassDoc> docsMap = new ConcurrentHashMap<>();

			//Multithreading is rather useless here... only saved 100 ms out of 400 of the time
			final ExecutorService service = Executors.newFixedThreadPool(4);
			for (Element element : document.select("body > main > ul > li > a")) {
				service.submit(() -> {
					try {
						final ClassDoc docs = ClassDocs.of(element.absUrl("href"));

						final ClassDoc oldVal = docsMap.put(docs.getClassName(), docs);

						if (oldVal != null) {
							throw new IllegalStateException("Duplicated docs: " + element.absUrl("href"));
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}

			service.shutdown();
			service.awaitTermination(1, TimeUnit.DAYS);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
