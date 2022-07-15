package com.freya02.docs

import org.jsoup.select.Elements

open class HTMLElementList protected constructor(elements: List<HTMLElement> = emptyList()) : ArrayList<HTMLElement>() {
    constructor(elements: Elements) : this() {
        for (element in elements) {
            this.add(HTMLElement.wrap(element))
        }
    }

    init {
        this.addAll(elements)
    }

    val htmlElements: List<HTMLElement>
        get() = this

    fun toMarkdown(delimiter: String): String = this.joinToString(delimiter) { obj: HTMLElement -> obj.markdown }

    fun toText(): String = this.joinToString("") { e: HTMLElement -> e.targetElement.text() }

    companion object {
        fun fromElements(elements: Elements): HTMLElementList {
            return HTMLElementList(elements)
        }
    }
}