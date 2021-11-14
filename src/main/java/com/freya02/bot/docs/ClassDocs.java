package com.freya02.bot.docs;

import com.freya02.bot.utils.HTMLElement;
import org.jsoup.nodes.Document;

import java.util.List;

public class ClassDocs extends BasicDocs {
	protected ClassDocs(Document document, HTMLElement description, HTMLElement classDecl, List<Detail> details) {
		super(document, description, classDecl, details);
	}
}
