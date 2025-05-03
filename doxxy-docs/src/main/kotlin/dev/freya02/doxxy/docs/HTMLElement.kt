package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.utils.HttpUtils.doesStartByLocalhost
import dev.freya02.doxxy.docs.utils.JDocUtil
import org.jetbrains.annotations.Contract
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.select.NodeVisitor

class HTMLElement private constructor(val targetElement: Element) {
    val asMarkdown: String by lazy {
        targetElement.traverse(object : NodeVisitor {
            override fun head(node: Node, depth: Int) {
                if (node is Element) {
                    if (!node.tagName().equals("a", ignoreCase = true)) {
                        if (node.tagName().equals("sub", ignoreCase = true)) {
                            node.text().replaceScriptCharactersOrNull(subscripts)?.let { node.text(it) }
                        } else if (node.tagName().equals("sup", ignoreCase = true)) {
                            node.text().replaceScriptCharactersOrNull(superscripts)?.let { node.text(it) }
                        }

                        return
                    }

                    val href = node.absUrl("href")

                    //Try to resolve into an online link
                    for (type in DocSourceType.entries) {
                        val effectiveUrl = type.toEffectiveURL(href)
                        if (!doesStartByLocalhost(effectiveUrl)) { //If it's a valid link then don't remove it
                            node.attr("href", effectiveUrl)
                            return
                        }
                    }

                    //If no online URL has been found then do not link to localhost href(s)
                    node.removeAttr("href")
                }
            }

            override fun tail(node: Node, depth: Int) {}
        })

        val html = targetElement.outerHtml()
//				.replaceAll("<code>([^/]*?)<a href=\"(.*?)\">(\\X*?)</a>([^/]*?)</code>", "<code>$1</code><a href=\"$2\"><code>$1</code></a><code>right</code>");

         JDocUtil.formatText(html, targetElement.baseUri())
    }

    override fun toString(): String {
        return targetElement.wholeText()
    }

    companion object {
        // fun
        // https://lingojam.com/SuperscriptGenerator
        val superscripts = "abcdefghijklmnopqrstuvwxyz0123456789+-=()".zip("ᵃᵇᶜᵈᵉᶠᵍʰᶦʲᵏˡᵐⁿᵒᵖᑫʳˢᵗᵘᵛʷˣʸᶻ⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻⁼⁽⁾") //uhh ok
            .associate { (k, v) -> k to v } + mapOf(
            // https://en.wikipedia.org/wiki/Unicode_subscripts_and_superscripts#Superscripts_and_subscripts_block
            '2' to '\u00B2',
            '3' to '\u00B3',
            '1' to '\u00B9',

            '0' to '\u2070',
            'i' to '\u2071',
            '4' to '\u2074',
            '5' to '\u2075',
            '6' to '\u2076',
            '7' to '\u2077',
            '8' to '\u2078',
            '9' to '\u2079',
            '+' to '\u207A',
            '-' to '\u207B',
            '=' to '\u207C',
            '(' to '\u207D',
            ')' to '\u207E',
            'n' to '\u207F',

            'x' to '\u02E3',
            's' to '\u02E2',
            'c' to '\u1D9C',
        )

        // even funnier
        // https://lingojam.com/SubscriptGenerator
        val subscripts = "abcdefghijklmnopqrstuvwxyz0123456789+-=()".zip("ₐ₆꜀ₔₑբ₉ₕᵢⱼₖₗₘₙₒₚqᵣₛₜᵤᵥᵥᵥₓᵧ₂₀₁₂₃₄₅₆₇₈₉₊₋₌₍₎") //wtf
            .associate { (k, v) -> k to v } + mapOf(
            // https://en.wikipedia.org/wiki/Unicode_subscripts_and_superscripts#Superscripts_and_subscripts_block
            '0' to '\u2080',
            '1' to '\u2081',
            '2' to '\u2082',
            '3' to '\u2083',
            '4' to '\u2084',
            '5' to '\u2085',
            '6' to '\u2086',
            '7' to '\u2087',
            '8' to '\u2088',
            '9' to '\u2089',
            '+' to '\u208A',
            '-' to '\u208B',
            '=' to '\u208C',
            '(' to '\u208D',
            ')' to '\u208E',

            'a' to '\u2090',
            'e' to '\u2091',
            'o' to '\u2092',
            'x' to '\u2093',
            'ə' to '\u2094',
            'h' to '\u2095',
            'k' to '\u2096',
            'l' to '\u2097',
            'm' to '\u2098',
            'n' to '\u2099',
            'p' to '\u209A',
            's' to '\u209B',
            't' to '\u209C',
        )

        fun String.replaceScriptCharactersOrNull(mappings: Map<Char, Char>): String? =
            lowercase()
                .toCharArray()
                .map { c -> mappings[c] ?: return null } //If you can't convert everything, abort
                .joinToString("")

        @Contract("null -> fail; !null -> new", pure = true)
        fun wrap(targetElement: Element?): HTMLElement = when (targetElement) {
           null -> throw DocParseException()
           else -> HTMLElement(targetElement)
       }

        @Contract(value = "null -> null; !null -> new", pure = true)
        fun tryWrap(targetElement: Element?): HTMLElement? = when {
            targetElement != null -> HTMLElement(targetElement)
            else -> null
        }
    }
}