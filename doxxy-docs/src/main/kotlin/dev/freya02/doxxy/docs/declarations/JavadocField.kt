package dev.freya02.doxxy.docs.declarations

import dev.freya02.doxxy.docs.JavadocElement
import dev.freya02.doxxy.docs.JavadocElements
import dev.freya02.doxxy.docs.exceptions.DocParseException
import dev.freya02.doxxy.docs.sections.ClassDetailType
import dev.freya02.doxxy.docs.sections.DetailToElementsMap
import dev.freya02.doxxy.docs.sections.DocDetail
import dev.freya02.doxxy.docs.sections.SeeAlso
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Element

private val logger = KotlinLogging.logger { }

class JavadocField internal constructor(
    val declaringClass: JavadocClass,
    val classDetailType: ClassDetailType,
    element: Element
) : AbstractJavadoc() {
    override val onlineURL: String?

    val fieldName: String
    val fieldType: String
    val fieldValue: String?

    override val descriptionElements: JavadocElements
    override val deprecationElement: JavadocElement?

    val elementId: String
    val modifiers: String

    override val detailToElementsMap: DetailToElementsMap

    override val seeAlso: SeeAlso?

    init {
        elementId = element.id()
        onlineURL = declaringClass.onlineURL?.let { "$it#$elementId" }

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
        descriptionElements = JavadocElements.fromElements(element.select("section.detail > div.block"))

        //Get field possible's deprecation
        deprecationElement = JavadocElement.tryWrap(element.selectFirst("section.detail > div.deprecation-block"))

        //Details
        detailToElementsMap = DetailToElementsMap.parseDetails(element)

        //See also
        val seeAlsoDetail = detailToElementsMap.getDetail(DocDetail.Type.SEE_ALSO)
        seeAlso = when {
            seeAlsoDetail != null -> SeeAlso(declaringClass.moduleSession, seeAlsoDetail)
            else -> null
        }

        fieldValue = when {
            "static" in modifiers //public might be omitted in interface constants
                    && "final" in modifiers
                    && seeAlso != null
                    && seeAlso.references.any { it.text == "Constant Field Values" && it.link.contains("/constant-values.html") } -> {
                val constantsMap = declaringClass.moduleSession.getConstantsOrNull(declaringClass.classNameFqcn)
                when {
                    constantsMap != null -> constantsMap[fieldName]
                    else -> {
                        logger.warn { "Could not find constants in ${declaringClass.classNameFqcn}" }
                        null
                    }
                }
            }
            else -> null
        }
    }

    override fun toString(): String {
        return "$fieldType $fieldName : $descriptionElements"
    }

    val simpleSignature: String
        get() = fieldName

    override val className: String
        get() = declaringClass.className

    override val identifier: String
        get() = simpleSignature
    override val identifierNoArgs: String
        get() = simpleSignature
    override val humanIdentifier: String
        get() = simpleSignature

    override val returnType: String
        get() = fieldType

    override fun toHumanClassIdentifier(className: String): String = "$className#$simpleSignature"
}