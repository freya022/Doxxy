package com.freya02.bot;

import com.freya02.bot.db.Database;
import com.freya02.bot.docs.cached.CachedClass;
import com.freya02.bot.docs.cached.CachedField;
import com.freya02.bot.docs.cached.CachedMethod;
import com.freya02.bot.docs.index.DocIndexKt;
import com.freya02.docs.DocSourceType;
import com.freya02.docs.DocWebServer;

import java.util.Collection;

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

		final DocIndexKt docIndexKt = new DocIndexKt(DocSourceType.BOT_COMMANDS, database);
//		docIndexKt.reindex();
		final CachedClass cachedClass = docIndexKt.getClassDoc("AppOption");
		final CachedMethod cachedMethod = docIndexKt.getMethodDoc("AppOption#autocomplete()");
		final CachedField cachedField = docIndexKt.getFieldDoc("AppendMode#SET");
		final Collection<String> methodSignatures = docIndexKt.findMethodSignatures("AppOption");
		final Collection<String> allMethodSignatures = docIndexKt.getAllMethodSignatures();
		final Collection<String> fieldSignatures = docIndexKt.findFieldSignatures("AppendMode");
		final Collection<String> allFieldSignatures = docIndexKt.getAllFieldSignatures();
		final Collection<String> methodAndFieldSignatures = docIndexKt.findMethodAndFieldSignatures("ApplicationCommandInfoMapView");

		System.out.println();

		System.exit(0);
	}
}
