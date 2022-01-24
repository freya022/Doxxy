package com.freya02.bot.docs;

import com.freya02.bot.docs.index.DocIndex;
import com.freya02.docs.DocSourceType;

import java.io.IOException;
import java.util.EnumMap;

public class DocIndexMap extends EnumMap<DocSourceType, DocIndex> {
	private static DocIndexMap instance;

	private DocIndexMap() {
		super(DocSourceType.class);
	}

	public static synchronized DocIndexMap getInstance() throws IOException {
		if (instance == null) {
			instance = new DocIndexMap();

			refreshIndex(DocSourceType.BOT_COMMANDS);
			refreshIndex(DocSourceType.JDA);
		}

		return instance;
	}

	public static synchronized void refreshIndex(DocSourceType sourceType) throws IOException {
		instance.put(sourceType, new DocIndex(sourceType));
	}
}
