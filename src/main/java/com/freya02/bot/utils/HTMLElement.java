package com.freya02.bot.utils;

import com.freya02.docs.DocParseException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

public class HTMLElement {
	private final Element targetElement;

	private HTMLElement(@NotNull Element targetElement) {
		this.targetElement = targetElement;
	}

	@Contract("null -> fail; !null -> new")
	@NotNull
	public static HTMLElement wrap(@Nullable Element targetElement) {
		if (targetElement == null) {
			throw new DocParseException();
		}

		return new HTMLElement(targetElement);
	}

	@Contract(value = "null -> null; !null -> new", pure = true)
	@Nullable
	public static HTMLElement tryWrap(@Nullable Element targetElement) {
		if (targetElement == null) {
			return null;
		}

		return new HTMLElement(targetElement);
	}

	public Element getTargetElement() {
		return targetElement;
	}

	//<a> inside <code> are not rendered to links :/
	public String getMarkdown() {
		targetElement.traverse(new NodeVisitor() {
			@Override
			public void head(Node node, int depth) {
				if (HttpUtils.doesStartByLocalhost(node.absUrl("href"))) {
					node.removeAttr("href");
				}
			}

			@Override
			public void tail(Node node, int depth) {}
		});

		final String html = targetElement.outerHtml();
//				.replaceAll("<code>([^/]*?)<a href=\"(.*?)\">(\\X*?)</a>([^/]*?)</code>", "<code>$1</code><a href=\"$2\"><code>$1</code></a><code>right</code>");


		return JDocUtil.formatText(html, targetElement.baseUri());
	}

	@Override
	public String toString() {
		return targetElement.wholeText();
	}
}
