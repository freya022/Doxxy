package dev.freya02.doxxy.docs.declarations

import dev.freya02.doxxy.docs.JavadocElement
import dev.freya02.doxxy.docs.JavadocElements
import dev.freya02.doxxy.docs.exceptions.DocParseException
import dev.freya02.doxxy.docs.sections.ClassDetailType
import dev.freya02.doxxy.docs.sections.DetailToElementsMap
import dev.freya02.doxxy.docs.sections.DocDetail
import dev.freya02.doxxy.docs.sections.SeeAlso
import dev.freya02.doxxy.docs.utils.DocUtils
import dev.freya02.doxxy.docs.utils.requireDoc
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import org.jsoup.nodes.Element

class JavadocMethod internal constructor(
    val declaringClass: JavadocClass,
    val classDetailType: ClassDetailType,
    element: Element
) : AbstractJavadoc() {
    val elementId: String
    val methodAnnotations: String?
    val methodName: String
    val methodSignature: String
    val methodParameters: MethodDocParameters?
    val methodReturnType: String

    val isStatic: Boolean

    override val descriptionElements: JavadocElements
    override val deprecationElement: JavadocElement?

    override val detailToElementsMap: DetailToElementsMap

    override val seeAlso: SeeAlso?

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
        methodParameters = methodParametersElement?.let { MethodDocParameters(it.text()) }

        val methodReturnTypeElement = element.selectFirst("div.member-signature > span.return-type")
        methodReturnType = when (methodReturnTypeElement) {
            null -> requireDoc(classDetailType == ClassDetailType.CONSTRUCTOR).let { declaringClass.className }
            else -> methodReturnTypeElement.text()
        }

        val modifiersElement = element.selectFirst("div.member-signature > span.modifiers")
        isStatic = modifiersElement != null && "static" in modifiersElement.text()

        //Get method description
        descriptionElements = JavadocElements.fromElements(element.select("section.detail > div.block"))

        //Get method possible's deprecation
        deprecationElement = JavadocElement.tryWrap(element.selectFirst("section.detail > div.deprecation-block"))

        //Need to parse the children of the <dl> tag in order to make a map of dt[class] -> List<Element>
        detailToElementsMap = DetailToElementsMap.parseDetails(element)

        //See also
        val seeAlsoDetail = detailToElementsMap.getDetail(DocDetail.Type.SEE_ALSO)
        seeAlso = when {
            seeAlsoDetail != null -> SeeAlso(declaringClass.moduleSession, seeAlsoDetail)
            else -> null
        }
    }

    val simpleSignature: String
        get() = DocUtils.getSimpleSignature(elementId)

    override val className: String
        get() = declaringClass.className

    override val identifier: String
        get() = simpleSignature
    override val identifierNoArgs: String
        get() = methodName
    override val humanIdentifier: String by lazy { generateHumanIdentifier("") }
    override fun toHumanClassIdentifier(className: String): String = generateHumanIdentifier("$className#")

    fun getSimpleAnnotatedSignature(targetClassdoc: JavadocClass): String {
        return DocUtils.getSimpleAnnotatedSignature(targetClassdoc, this)
    }

    private fun MethodDocParameters.asParametersString(numTypes: Int): String {
        var i = 0
        return this.parameters.joinToString { param ->
            i++

            when {
                i <= numTypes -> param.simpleType + " " + param.name
                else -> param.name
            }
        }
    }

    private fun generateHumanIdentifier(initialString: String) = buildString {
        append(initialString)

        append(methodName)
        append('(')
        if (methodParameters != null) {
            for (numTypes in methodParameters.parameters.size downTo 0) {
                val asParametersString = methodParameters.asParametersString(numTypes)
                if (asParametersString.length + this.length + 1 <= Choice.MAX_NAME_LENGTH) {
                    append(asParametersString)
                    break
                }
            }
        }
        append(')')
    }

    override val onlineURL: String? = declaringClass.onlineURL?.let { "$it#$elementId" }

    override val returnType: String
        get() = methodReturnType

    override fun toString(): String {
        return "$methodSignature : $descriptionElements"
    }
}