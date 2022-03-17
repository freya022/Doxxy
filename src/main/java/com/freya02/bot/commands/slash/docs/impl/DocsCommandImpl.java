package com.freya02.bot.commands.slash.docs.impl;

import com.freya02.bot.commands.slash.docs.CommonDocsHandlers;
import com.freya02.bot.docs.DocIndexMap;
import com.freya02.bot.docs.index.DocIndex;
import com.freya02.botcommands.api.application.slash.GuildSlashEvent;
import com.freya02.docs.DocSourceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class DocsCommandImpl {
	private final DocIndexMap docIndexMap;

	public DocsCommandImpl() throws IOException {
		this.docIndexMap = DocIndexMap.getInstance();
	}

	public void onSlashDocs(@NotNull GuildSlashEvent event,
	                        @NotNull DocSourceType sourceType,
	                        @NotNull String className,
	                        @Nullable String identifier) throws IOException {

		final DocIndex docIndex = docIndexMap.get(sourceType);

		if (identifier == null) {
			CommonDocsHandlers.handleClass(event, className, docIndex);
		} else if (identifier.contains("(")) { //prob a method
			CommonDocsHandlers.handleMethodDocs(event, className, identifier, docIndex);
		} else {
			CommonDocsHandlers.handleFieldDocs(event, className, identifier, docIndex);
		}
	}
}
