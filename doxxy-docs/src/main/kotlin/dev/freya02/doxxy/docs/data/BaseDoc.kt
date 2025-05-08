package dev.freya02.doxxy.docs.data

import dev.freya02.doxxy.docs.HTMLElement
import dev.freya02.doxxy.docs.HTMLElementList
import java.util.*
import java.util.regex.Pattern

abstract class BaseDoc {
    abstract val effectiveURL: String
    abstract val onlineURL: String?
    abstract val descriptionElements: HTMLElementList
    abstract val deprecationElement: HTMLElement?
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

    fun getDetails(includedTypes: EnumSet<DocDetailType>): List<DocDetail> =
        DocDetailType.entries
            .filter { it in includedTypes }
            .mapNotNull { detailType -> detailToElementsMap.getDetail(detailType) }
}

private val ANNOTATION_PATTERN = Pattern.compile("@\\w*")
val BaseDoc.returnTypeNoAnnotations: String?
    get() = returnType?.let {
        ANNOTATION_PATTERN.matcher(it)
            .replaceAll("")
            .trim()
    }