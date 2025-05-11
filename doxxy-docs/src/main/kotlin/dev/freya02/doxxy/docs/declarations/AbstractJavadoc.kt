package dev.freya02.doxxy.docs.declarations

import dev.freya02.doxxy.docs.JavadocElement
import dev.freya02.doxxy.docs.JavadocElements
import dev.freya02.doxxy.docs.sections.DetailToElementsMap
import dev.freya02.doxxy.docs.sections.DocDetail
import dev.freya02.doxxy.docs.sections.SeeAlso
import java.util.*
import java.util.regex.Pattern

abstract class AbstractJavadoc internal constructor() {
    abstract val onlineURL: String?
    abstract val descriptionElements: JavadocElements
    abstract val deprecationElement: JavadocElement?
    abstract val seeAlso: SeeAlso?

    abstract val className: String
    /** method(Type1, Type2) */
    abstract val identifier: String?
    /** method */
    abstract val identifierNoArgs: String?
    /** method(Type name, name2) */
    abstract val humanIdentifier: String?
    abstract fun toHumanClassIdentifier(className: String): String?
    abstract val returnType: String?

    protected abstract val detailToElementsMap: DetailToElementsMap

    fun getDetails(includedTypes: EnumSet<DocDetail.Type>): List<DocDetail> =
        DocDetail.Type.entries
            .filter { it in includedTypes }
            .mapNotNull { detailType -> detailToElementsMap.getDetail(detailType) }
}

private val ANNOTATION_PATTERN = Pattern.compile("@\\w*")
val AbstractJavadoc.returnTypeNoAnnotations: String?
    get() = returnType?.let {
        ANNOTATION_PATTERN.matcher(it)
            .replaceAll("")
            .trim()
    }