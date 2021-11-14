package com.freya02.bot.docs;

import com.freya02.bot.utils.HTMLElement;
import org.jsoup.nodes.Document;

import java.util.List;

public class EnumDocs extends BasicDocs {
	protected EnumDocs(Document document, HTMLElement description, HTMLElement classDecl, List<Detail> details) {
		super(document, description, classDecl, details);
	}
}
