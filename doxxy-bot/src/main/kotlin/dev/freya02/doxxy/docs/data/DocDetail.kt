package dev.freya02.doxxy.docs.data

import dev.freya02.doxxy.docs.HTMLElement
import dev.freya02.doxxy.docs.HTMLElementList

class DocDetail(detailType: DocDetailType, htmlElements: List<HTMLElement>) : HTMLElementList(htmlElements) {
    val detailString: String = detailType.elementText
}