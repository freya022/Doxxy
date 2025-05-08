@file:Suppress("JavaDefaultMethodsNotOverriddenByDelegation")

package dev.freya02.doxxy.docs

import org.jsoup.select.Elements

open class HTMLElementList protected constructor(elements: List<HTMLElement>) : List<HTMLElement> by elements {
    internal val htmlElements: List<HTMLElement>
        get() = this

    fun toMarkdown(delimiter: String): String = this.joinToString(delimiter) { obj: HTMLElement -> obj.asMarkdown }

    override fun toString(): String = joinToString("") { e: HTMLElement -> e.targetElement.text() }

    internal companion object {
        internal fun fromElements(elements: Elements): HTMLElementList {
            return HTMLElementList(elements.map(HTMLElement::wrap))
        }
    }
}