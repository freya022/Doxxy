package com.freya02.docs.data

import com.freya02.docs.HTMLElement
import com.freya02.docs.HTMLElementList
import java.util.*

abstract class BaseDoc {
    abstract val effectiveURL: String
    abstract val descriptionElements: HTMLElementList
    abstract val deprecationElement: HTMLElement?

    protected abstract val detailToElementsMap: DetailToElementsMap

    fun getDetails(includedTypes: EnumSet<DocDetailType>): List<DocDetail> =
        DocDetailType.values()
            .filter { it in includedTypes }
            .mapNotNull { detailType -> detailToElementsMap.getDetail(detailType) }
}