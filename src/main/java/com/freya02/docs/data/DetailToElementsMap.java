package com.freya02.docs.data;

import com.freya02.bot.utils.HTMLElement;
import com.freya02.botcommands.api.Logging;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;

import java.util.*;

public class DetailToElementsMap {
	private static final Logger LOGGER = Logging.getLogger();
	private static final Set<String> warned = new HashSet<>();

	private final Map<DocDetailType, List<HTMLElement>> map = new HashMap<>();

	private DetailToElementsMap(Element detailTarget) {
		List<HTMLElement> list = null;

		for (Element element : detailTarget.select("dl.notes")) {
			for (Element child : element.children()) {
				final String tagName = child.tag().normalName();

				if (tagName.equals("dt")) {
					final String detailName = child.text();
					final DocDetailType type = DocDetailType.parseType(detailName);
					if (type == null) {
						if (warned.add(detailName)) {
							LOGGER.warn("Unknown method detail type: '{}' at {}", detailName, detailTarget.baseUri());
						}

						list = null;
					} else {
						list = map.computeIfAbsent(type, s -> new ArrayList<>());
					}
				} else if (tagName.equals("dd") && list != null) {
					list.add(new HTMLElement(child));
				}
			}
		}
	}

	public static DetailToElementsMap parseDetails(@NotNull Element detailTarget) {
		return new DetailToElementsMap(detailTarget);
	}

	@Nullable
	public DocDetail getDetail(DocDetailType detailType) {
		final List<HTMLElement> elements = map.get(detailType);
		if (elements == null) return null;

		return new DocDetail(detailType, elements);
	}
}
