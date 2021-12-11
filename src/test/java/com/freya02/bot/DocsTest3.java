package com.freya02.bot;

import com.freya02.docs.ClassDocs;

import java.io.IOException;

public class DocsTest3 {
	public static void main(String[] args) throws IOException {
//		TestUtils.measureTime("get all docs", 10, 10, DocsTest3::getAllDocs);

		getAllDocs("http://localhost:63342/DocsBot/test_docs/allclasses.html");

//		final ClassDocs docs1 = new ClassDocs("https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/zip/ZipEntry.html");
//		final ClassDoc docs2 = ClassDocs.of("https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/AbstractCollection.html");
//		final ClassDoc docs3 = ClassDocs.of("https://docs.oracle.com/en/java/javase/16/docs/api/java.desktop/java/awt/TextField.html");

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
}
