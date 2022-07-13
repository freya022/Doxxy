package com.freya02.bot;

import com.freya02.bot.db.Database;
import com.freya02.bot.docs.index.DocIndexKt;
import com.freya02.docs.DocSourceType;
import com.freya02.docs.DocWebServer;

public class DocIndexTest {
	public static void main(String[] args) throws Exception {
		DocWebServer.startDocWebServer();

//		final DocIndex bcIndex = new DocIndex(DocSourceType.BOT_COMMANDS).reindex();
//		final DocIndex jdaIndex = new DocIndex(DocSourceType.JDA).reindex();
//		final DocIndex javaIndex = new DocIndex(DocSourceType.JAVA).reindex();
//
//		System.out.println();
//
//		bcIndex.close();
//		jdaIndex.close();
//		javaIndex.close();

		final Config config = Config.Companion.getConfig();
		final Database database = new Database(config);

		final DocIndexKt docIndexKt = new DocIndexKt(DocSourceType.BOT_COMMANDS, database).reindex();

		System.out.println();
	}
}
