package com.freya02.docs.data

import com.freya02.docs.DocParseException
import com.freya02.docs.DocUtils
import com.freya02.docs.HTMLElement
import com.freya02.docs.HTMLElementList
import com.freya02.docs.utils.requireDoc
import org.jsoup.nodes.Element

class MethodDoc(val classDocs: ClassDoc, val classDetailType: ClassDetailType, element: Element) : BaseDoc() {
    val elementId: String
    val methodAnnotations: String?
    val methodName: String
    val methodSignature: String
    val methodParameters: String?
    val methodReturnType: String

    override val descriptionElements: HTMLElementList
    override val deprecationElement: HTMLElement?

    override val detailToElementsMap: DetailToElementsMap

    val seeAlso: SeeAlso?

    init {
        elementId = element.id()

        //Get method name
        val methodNameElement = element.selectFirst("h3") ?: throw DocParseException()
        methodName = methodNameElement.text()

        //Get method signature
        val methodSignatureElement = element.selectFirst("div.member-signature") ?: throw DocParseException()
        methodSignature = methodSignatureElement.text()

        val methodAnnotationsElement = element.selectFirst("div.member-signature > span.annotations")
        methodAnnotations = methodAnnotationsElement?.text()

        val methodParametersElement = element.selectFirst("div.member-signature > span.parameters")
        methodParameters = methodParametersElement?.text()

        val methodReturnTypeElement = element.selectFirst("div.member-signature > span.return-type")
        methodReturnType = when (methodReturnTypeElement) {
            null -> requireDoc(classDetailType == ClassDetailType.CONSTRUCTOR).let { classDocs.className }
            else -> methodReturnTypeElement.text()
        }

        //Get method description
        descriptionElements = HTMLElementList.fromElements(element.select("section.detail > div.block"))

        //Get method possible's deprecation
        deprecationElement = HTMLElement.tryWrap(element.selectFirst("section.detail > div.deprecation-block"))

        //Need to parse the children of the <dl> tag in order to make a map of dt[class] -> List<Element>
        detailToElementsMap = DetailToElementsMap.parseDetails(element)

        //See also
        val seeAlsoDetail = detailToElementsMap.getDetail(DocDetailType.SEE_ALSO)
        seeAlso = when {
            seeAlsoDetail != null -> SeeAlso(classDocs.source, seeAlsoDetail)
            else -> null
        }
    }

    val simpleSignature: String
        get() = DocUtils.getSimpleSignature(elementId)

    fun getSimpleAnnotatedSignature(targetClassdoc: ClassDoc): String {
        return DocUtils.getSimpleAnnotatedSignature(targetClassdoc, this)
    }

    override val effectiveURL: String
        get() = classDocs.effectiveURL + '#' + elementId

    override fun toString(): String {
        return "$methodSignature : $descriptionElements"
    }
}