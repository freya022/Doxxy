package dev.freya02.doxxy.docs.declarations

import dev.freya02.doxxy.docs.JavadocElement
import dev.freya02.doxxy.docs.JavadocElements
import dev.freya02.doxxy.docs.sections.DetailToElementsMap
import dev.freya02.doxxy.docs.sections.DocDetail
import dev.freya02.doxxy.docs.sections.SeeAlso
import java.util.*

abstract class AbstractJavadoc internal constructor() {
    abstract val onlineURL: String?
    abstract val descriptionElements: JavadocElements
    abstract val deprecationElement: JavadocElement?
    abstract val seeAlso: SeeAlso?

    abstract val className: String

    internal abstract val detailToElementsMap: DetailToElementsMap

    fun getDetails(includedTypes: EnumSet<DocDetail.Type>): List<DocDetail> =
        DocDetail.Type.entries
            .filter { it in includedTypes }
            .mapNotNull { detailType -> detailToElementsMap.getDetail(detailType) }
}
