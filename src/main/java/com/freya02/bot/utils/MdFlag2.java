package com.freya02.bot.utils;

import org.jsoup.nodes.Node;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public enum MdFlag2 {
	PARAGRAPH(n -> "\n", n -> "\n", "p"),
	PRE(n -> "", n -> "", "pre"),
	CODE(n -> "`", n -> "`", "code"),
	LINK(n -> "[", n -> {
		while (!n.hasAttr("href") && n.parentNode() != null) {
			n = n.parentNode();
		}

		return "](%s)".formatted(n.absUrl("href"));
	}, "a"),
	BOLD(n -> "**", n -> "**", "b", "strong", "h2"),
	STRIKETHROUGH(n -> "~~", n -> "~~", "s"),
	ITALIC(n -> "*", n -> "*", "i"),
	LINKED_CODE(n -> "[`", n -> {
		while (!n.hasAttr("href") && n.parentNode() != null) {
			n = n.parentNode();
		}

		return "`](%s)".formatted(n.absUrl("href"));
	}, "linkedcode"), FENCED_CODE(n -> "```java\n", n -> "```", "fencedcode");

	private final Function<Node, String> prefix;
	private final Function<Node, String> suffix;
	private final List<String> tags;

	MdFlag2(Function<Node, String> prefix, Function<Node, String> suffix, String... tags) {
		this.prefix = prefix;
		this.suffix = suffix;
		this.tags = Arrays.asList(tags);
	}

	public List<String> getTags() {
		return tags;
	}

	public String getPrefix(Node node) {
		return prefix.apply(node);
	}

	public String getSuffix(Node node) {
		return suffix.apply(node);
	}
}
