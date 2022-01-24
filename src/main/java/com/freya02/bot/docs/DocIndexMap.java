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

			refreshIndex(DocSourceType.BOT_COMMANDS);
			refreshIndex(DocSourceType.JDA);
		}

		return instance;
	}

	private static synchronized void refreshIndex(DocSourceType sourceType) throws IOException {
		final DocIndex oldIndex = instance.put(sourceType, new DocIndex(sourceType));

		if (oldIndex != null) {
			oldIndex.close();
		}
	}

	public static synchronized void refreshAndInvalidateIndex(DocSourceType sourceType) throws IOException {
		final DocIndex oldIndex = instance.remove(sourceType);

		if (oldIndex != null) {
			oldIndex.closeAndDelete();
		}

		//TODO try to rework the invalidation mechanism
		// This could lead to errors if there are user interactions during the reconstruction
		instance.put(sourceType, new DocIndex(sourceType));
	}
}
