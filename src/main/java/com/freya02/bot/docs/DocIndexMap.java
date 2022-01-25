package com.freya02.bot.docs;

import com.freya02.bot.docs.index.DocIndex;
import com.freya02.botcommands.api.Logging;
import com.freya02.docs.DocSourceType;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.EnumMap;

public class DocIndexMap extends EnumMap<DocSourceType, DocIndex> {
	private static final Logger LOGGER = Logging.getLogger();
	private static DocIndexMap instance;

	private DocIndexMap() {
		super(DocSourceType.class);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				for (DocIndex index : values()) {
					index.close();
				}
			} catch (IOException e) {
				LOGGER.error("Unable to close cache", e);
			}
		}));
	}

	public static synchronized DocIndexMap getInstance() throws IOException {
		if (instance == null) {
			instance = new DocIndexMap();

			instance.put(DocSourceType.BOT_COMMANDS, new DocIndex(DocSourceType.BOT_COMMANDS));
			instance.put(DocSourceType.JDA, new DocIndex(DocSourceType.JDA));
		}

		return instance;
	}

	public static synchronized void refreshAndInvalidateIndex(DocSourceType sourceType) throws IOException {
		final DocIndex index = instance.get(sourceType);

		if (index != null) {
			index.reindex(true);
		}
	}
}
