package com.freya02.docs.data

import com.freya02.docs.DocParseException
import com.freya02.docs.HTMLElement
import com.freya02.docs.HTMLElementList
import org.jsoup.nodes.Element

class FieldDoc(val classDocs: ClassDoc, val classDetailType: ClassDetailType, element: Element) : BaseDoc() {
    override val effectiveURL: String
    val fieldName: String
    val fieldType: String
    override val descriptionElements: HTMLElementList
    override val deprecationElement: HTMLElement?

    val elementId: String
    val modifiers: String

    override val detailToElementsMap: DetailToElementsMap

    override val seeAlso: SeeAlso?

    init {
        elementId = element.id()
        effectiveURL = classDocs.effectiveURL + "#" + elementId

        //Get field modifiers
        val modifiersElement = element.selectFirst("div.member-signature > span.modifiers") ?: throw DocParseException()
        modifiers = modifiersElement.text()

        //Get field name
        val fieldNameElement = element.selectFirst("h3") ?: throw DocParseException()
        fieldName = fieldNameElement.text()

        //Get field type
        val fieldTypeElement =
            element.selectFirst("div.member-signature > span.return-type") ?: throw DocParseException()
        fieldType = fieldTypeElement.text()

        //Get field description
        descriptionElements = HTMLElementList.fromElements(element.select("section.detail > div.block"))

        //Get field possible's deprecation
        deprecationElement = HTMLElement.tryWrap(element.selectFirst("section.detail > div.deprecation-block"))

        //Details
        detailToElementsMap = DetailToElementsMap.parseDetails(element)

        //See also
        val seeAlsoDetail = detailToElementsMap.getDetail(DocDetailType.SEE_ALSO)
        seeAlso = when {
            seeAlsoDetail != null -> SeeAlso(classDocs.source, seeAlsoDetail)
            else -> null
        }
    }

    override fun toString(): String {
        return "$fieldType $fieldName : $descriptionElements"
    }

    val simpleSignature: String
        get() = fieldName

    override val className: String
        get() = classDocs.className

    override val identifier: String
        get() = simpleSignature
    override val identifierNoArgs: String
        get() = simpleSignature
    override val humanIdentifier: String
        get() = simpleSignature

    override fun toHumanClassIdentifier(className: String): String = "$className#$simpleSignature"
}