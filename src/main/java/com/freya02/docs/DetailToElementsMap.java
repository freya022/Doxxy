package com.freya02.docs;

import com.freya02.bot.utils.HTMLElement;
import com.freya02.botcommands.api.Logging;
import com.freya02.botcommands.internal.utils.ReflectionUtils;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;

import java.util.*;

public class DetailToElementsMap extends HashMap<DocDetailType, List<HTMLElement>> {
	private static final Logger LOGGER = Logging.getLogger();
	private static final Set<String> warned = new HashSet<>();

	private final Set<DocDetailType> got = new HashSet<>();

	private final Element detailTarget;

	public DetailToElementsMap(Element detailTarget) {
		this.detailTarget = detailTarget;

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
						list = computeIfAbsent(type, s -> new ArrayList<>());
					}
				} else if (tagName.equals("dd") && list != null) {
					list.add(new HTMLElement(child));
				}
			}
		}
	}

	public void onParseFinished() {
		final Collection<DocDetailType> keys = new ArrayList<>(keySet());
		keys.removeAll(got);

		if (keys.size() > 0) {
			if (warned.addAll(keys.stream().map(DocDetailType::name).toList())) {
				LOGGER.warn("Doc detail types were omitted at {} : {} for {}", ReflectionUtils.formatCallerMethod(), keys, detailTarget.baseUri());
			}
		}
	}

	@Override
	public List<HTMLElement> get(Object key) {
		this.got.add((DocDetailType) key);

		return super.get(key);
	}

	@Nullable
	public HTMLElement findFirst(DocDetailType detailType) {
		final List<HTMLElement> list = get(detailType);
		if (list == null || list.isEmpty()) return null;

		if (list.size() > 1) throw new IllegalStateException("findFirst was used on a list with more than 1 element");

		return list.get(0);
	}
}
