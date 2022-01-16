package com.freya02.bot.docs;

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

			instance.put(DocSourceType.BOT_COMMANDS, new DocIndex(DocSourceType.BOT_COMMANDS));
			instance.put(DocSourceType.JDA, new DocIndex(DocSourceType.JDA));
		}

		return instance;
	}
}
