package com.freya02.bot.utils;

import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

import java.util.*;

public class HTMLElement {
	private final Element targetElement;

	public HTMLElement(@NotNull Element targetElement) {
		this.targetElement = targetElement;
	}

	public Element getTargetElement() {
		return targetElement;
	}

	//<a> inside <code> are not rendered to links :/
	public String getMarkdown3() {
		final String html = targetElement.outerHtml();
//				.replaceAll("<code>([^/]*?)<a href=\"(.*?)\">(\\X*?)</a>([^/]*?)</code>", "<code>$1</code><a href=\"$2\"><code>$1</code></a><code>right</code>");


		return JDocUtil.formatText(html, targetElement.baseUri());
	}

	public String getMarkdown2(boolean allowStandaloneCode) {
		final StringBuilder builder = new StringBuilder();
		final LinkedHashMap<TextNode, TextAttributes> map = new LinkedHashMap<>();

		targetElement.traverse(new NodeVisitor() {
			private final Deque<MdFlag2> flags = new ArrayDeque<>();

			@Override
			public void head(@NotNull Node node, int depth) {
				if (node instanceof Element element) {
					if (element.tag().normalName().equals("br")) {
						map.put(TextNode.createFromEncoded("\n"), new TextAttributes(node, flags));

						return;
					}

					for (MdFlag2 flag : MdFlag2.values()) {
						final String tagName = element.tag().normalName();

						if (flag.getTags().contains(tagName)) {
							flags.addLast(flag);
						}
					}
				} else if (node instanceof TextNode textNode) {
					map.put(textNode, new TextAttributes(node, flags));
				} else {
					System.out.println("node = " + node);
					System.out.println("depth = " + depth);
				}
			}

			@Override
			public void tail(@NotNull Node node, int depth) {
				if (node instanceof Element element) {
					for (MdFlag2 flag : MdFlag2.values()) {
						final String tagName = element.tag().normalName();

						if (flag.getTags().contains(tagName)) {
							final MdFlag2 pop = flags.removeLast();

							if (!pop.getTags().contains(tagName)) {
								throw new IllegalArgumentException("Popped " + pop + " but had a end of </" + tagName + ">");
							}
						}
					}
				}
			}
		});

		for (TextAttributes attributes : map.values()) {
			if (attributes.contains(MdFlag2.CODE, MdFlag2.LINK)) {
				final List<MdFlag2> flags = attributes.flags();
				flags.remove(MdFlag2.CODE);
				flags.remove(MdFlag2.LINK);

				flags.add(MdFlag2.LINKED_CODE);
			} else if (attributes.contains(MdFlag2.CODE, MdFlag2.PRE)) {
				attributes.flags().clear();
				attributes.flags().add(MdFlag2.FENCED_CODE);
			}
		}

		final List<MdFlag2> currentFlags = new ArrayList<>();
		map.forEach((textNode, textAttributes) -> {
			if (textAttributes.contains(MdFlag2.CODE) && !(textAttributes.contains(MdFlag2.PRE) || textAttributes.contains(MdFlag2.LINK))) {
				if (!allowStandaloneCode) {
					textAttributes.flags().remove(MdFlag2.CODE);
				}
			}

			final List<MdFlag2> addedFlags = new ArrayList<>(textAttributes.flags());
			addedFlags.removeAll(currentFlags);

			final List<MdFlag2> removedFlags = new ArrayList<>(currentFlags);
			removedFlags.removeAll(textAttributes.flags());

			currentFlags.clear();
			currentFlags.addAll(textAttributes.flags());

			if (textNode.getWholeText().equals("\n")) {
				builder.append('\n');

				return;
			}

			for (MdFlag2 flag : removedFlags) {
				builder.append(flag.getSuffix(textAttributes.node()));
			}

			for (MdFlag2 flag : addedFlags) {
				builder.append(flag.getPrefix(textAttributes.node()));
			}

			if (textAttributes.contains(MdFlag2.FENCED_CODE)) {
				builder.append(textNode.getWholeText());
			} else {
				builder.append(textNode.text()
						.replace("<", "\\<") //Need to escape chevrons from java code
						.replace(">", "\\>")
				);
			}
		});

		return builder.toString();
	}

	public String getMarkdown() {
		final StringBuilder builder = new StringBuilder();

		targetElement.traverse(new NodeVisitor() {
			private final Stack<MdFlag> flags = new Stack<>();

			@Override
			public void head(@NotNull Node node, int depth) {
				if (node instanceof Element element) {
					if (element.tag().normalName().equals("br")) {
						builder.append('\n');

						return;
					}

					for (MdFlag flag : MdFlag.values()) {
						final String tagName = element.tag().normalName();

						if (flag.getTags().contains(tagName)) {
							flags.push(flag);

							if (flags.contains(MdFlag.PRE) && flag == MdFlag.CODE) {
								builder.append("```java\n");
							} else {
								builder.append(flag.getPrefix(node));
							}
						}
					}
				} else if (node instanceof TextNode textNode) {
					if (flags.contains(MdFlag.PRE)) {
						builder.append(textNode.getWholeText());
					} else {
						builder.append(textNode.text());
					}
				} else {
					System.out.println("node = " + node);
					System.out.println("depth = " + depth);
				}
			}

			@Override
			public void tail(@NotNull Node node, int depth) {
				if (node instanceof Element element) {
					for (MdFlag flag : MdFlag.values()) {
						final String tagName = element.tag().normalName();

						if (flag.getTags().contains(tagName)) {
							final MdFlag pop = flags.pop();
							if (!pop.getTags().contains(tagName)) {
								throw new IllegalArgumentException("Popped " + pop + " but had a end of </" + tagName + ">");
							}

							if (flags.contains(MdFlag.PRE) && flag == MdFlag.CODE) {
								builder.append("```\n");
							} else {
								builder.append(flag.getSuffix(node));
							}
						}
					}
				}
			}
		});

		return builder.toString().replaceAll("`\\[(.*?)]\\((.*?)\\)`", "[`$1`]($2)");
	}

	@Override
	public String toString() {
		return targetElement.wholeText();
	}
}
