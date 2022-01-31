package com.freya02.bot;

import com.freya02.bot.utils.HTMLElement;
import org.jsoup.Jsoup;

public class MdTest {
	public static void main(String[] args) {
//		final String html = "<b><i>lmao</i>not italic</b>not bold<br>newline <code><a href=\"mahlink.html\">Absolutely not a link</a></code>, <code><a href=\"mahlink.html\">Absolutely not a link</a></code>";
		final String html = "<dd><code><a href=\"https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html?is-external=true\" title=\"class or interface in java.io\" class=\"externalLink\">Serializable</a></code>, <code><a href=\"https://docs.oracle.com/javase/8/docs/api/java/lang/Comparable.html?is-external=true\" title=\"class or interface in java.lang\" class=\"externalLink\">Comparable</a>&lt;<a href=\"Component.Type.html\" title=\"enum in net.dv8tion.jda.api.interactions.components\">Component.Type</a>&gt;</code></dd>";

		testHtml(html);

		System.out.println("\n" + "-".repeat(50) + "\n");

		testHtml("<a href=\"Invite.html#getType()\"><code>Invite.getType()</code></a>");
	}

	private static void testHtml(String html) {
		final HTMLElement element = HTMLElement.wrap(Jsoup.parseBodyFragment(html, "https://myurl.com").selectFirst("body"));

		System.out.println(element.getMarkdown());
	}
}
