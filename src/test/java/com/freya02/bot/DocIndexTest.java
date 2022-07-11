package com.freya02.bot;

import com.freya02.bot.docs.index.DocIndex;
import com.freya02.docs.DocSourceType;
import com.freya02.docs.DocWebServer;

public class DocIndexTest {
	public static void main(String[] args) throws Exception {
		DocWebServer.startDocWebServer();

		final DocIndex bcIndex = new DocIndex(DocSourceType.BOT_COMMANDS).reindex();
		final DocIndex jdaIndex = new DocIndex(DocSourceType.JDA).reindex();
		final DocIndex javaIndex = new DocIndex(DocSourceType.JAVA).reindex();

		System.out.println();

		bcIndex.close();
		jdaIndex.close();
		javaIndex.close();
	}
}
