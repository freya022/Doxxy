package com.freya02.docs.data

import com.freya02.docs.HTMLElement
import com.freya02.docs.HTMLElementList

class DocDetail(detailType: DocDetailType, htmlElements: List<HTMLElement>) : HTMLElementList(htmlElements) {
    val detailString: String = detailType.elementText
}