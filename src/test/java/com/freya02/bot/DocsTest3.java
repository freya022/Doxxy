package com.freya02.bot;

import com.freya02.docs.ClassDocs;
import com.freya02.docs.DocSourceType;
import com.freya02.docs.DocWebServer;
import com.freya02.docs.DocsSession;
import com.freya02.docs.data.ClassDoc;

public class DocsTest3 {
	public static void main(String[] args) throws Exception {
//		final ClassDoc doc = new ClassDoc("http://localhost:63342/DocsBot/BotCommands_docs/com/freya02/botcommands/api/components/builder/LambdaComponentBuilder.html");
//		final ClassDoc doc2 = new ClassDoc("http://localhost:63342/DocsBot/BotCommands_docs/com/freya02/botcommands/api/components/builder/AbstractLambdaComponentBuilder.html");
//		final ClassDoc doc3 = new ClassDoc("http://localhost:63342/DocsBot/BotCommands_docs/com/freya02/botcommands/api/application/annotations/AppOption.html#description()");
//		final DocIndex bcIndex = new DocIndex(DocSourceType.BOT_COMMANDS).reindex();
//		final DocIndex jdaIndex = new DocIndex(DocSourceType.JDA).reindex();
//		final DocIndex javaIndex = new DocIndex(DocSourceType.JAVA).reindex();
//
//		bcIndex.close();
//		jdaIndex.close();
//		javaIndex.close();

//		final DocsSession session = new DocsSession();
//		final ClassDoc deprecationClassTest = session.retrieveDoc("https://docs.oracle.com/en/java/javase/17/docs/api/jdk.jartool/com/sun/jarsigner/ContentSigner.html");
//		final ClassDoc deprecationMethodTest = session.retrieveDoc("https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/System.html#getSecurityManager()");
//		final ClassDoc deprecationFieldTest = session.retrieveDoc("https://docs.oracle.com/en/java/javase/17/docs/api/jdk.accessibility/com/sun/java/accessibility/util/AWTEventMonitor.html#containerListener");
//		final ClassDoc arraysTest = session.retrieveDoc("https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/Arrays.html");
//		final ClassDoc enumTest = session.retrieveDoc("https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/StandardCopyOption.html");

		DocWebServer.startDocWebServer();
		final ClassDocs updatedSource = ClassDocs.getUpdatedSource(DocSourceType.JDA);

//		final Document document = HttpUtils.getDocument("https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/StandardCopyOption.html");

		final String url = updatedSource.getSimpleNameToUrlMap().get("MessageType");

		final ClassDoc onlineClassDoc = new ClassDoc(new DocsSession(), "https://ci.dv8tion.net/job/JDA5/javadoc/net/dv8tion/jda/api/entities/MessageType.html");
		final ClassDoc classDoc = new ClassDoc(new DocsSession(), url);

		System.out.println("onlineClassDoc.getDescriptionElement().getMarkdown() = " + onlineClassDoc.getDescriptionElement().getMarkdown());
		System.out.println("classDoc      .getDescriptionElement().getMarkdown() = " + classDoc.getDescriptionElement().getMarkdown());

		System.out.println();
	}
}
