@file:Suppress("JavaDefaultMethodsNotOverriddenByDelegation")

package dev.freya02.doxxy.docs

import org.jsoup.select.Elements

open class JavadocElements protected constructor(elements: List<JavadocElement>) : List<JavadocElement> by elements {

    override fun toString(): String = joinToString("") { e: JavadocElement -> e.targetElement.text() }

    internal companion object {
        internal fun fromElements(elements: Elements): JavadocElements {
            return JavadocElements(elements.map(JavadocElement::wrap))
        }
    }
}