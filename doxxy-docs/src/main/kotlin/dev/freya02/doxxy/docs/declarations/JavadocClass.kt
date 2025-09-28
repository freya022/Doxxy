package dev.freya02.doxxy.docs.declarations

import dev.freya02.doxxy.docs.JavadocElement
import dev.freya02.doxxy.docs.JavadocElements
import dev.freya02.doxxy.docs.JavadocModuleSession
import dev.freya02.doxxy.docs.exceptions.DocParseException
import dev.freya02.doxxy.docs.sections.ClassDetailType
import dev.freya02.doxxy.docs.sections.DetailToElementsMap
import dev.freya02.doxxy.docs.sections.DocDetail
import dev.freya02.doxxy.docs.sections.SeeAlso
import dev.freya02.doxxy.docs.utils.checkJavadocVersion
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException

private val logger = KotlinLogging.logger { }
private val loggedMissingSuperclassLinks = hashSetOf<String>()

class JavadocClass internal constructor(
    val moduleSession: JavadocModuleSession,
    val sourceURL: String,
    document: Document,
) : AbstractJavadoc() {

    val docTitleElement: JavadocElement
    val classNameFqcn: String by lazy { "$packageName.$className" }
    val packageName: String
    override val className: String
    override val descriptionElements: JavadocElements
    override val deprecationElement: JavadocElement?

    override val detailToElementsMap: DetailToElementsMap

    override val seeAlso: SeeAlso?

    private val _fields: MutableMap<String, JavadocField> = hashMapOf()
    val fields: Map<String, JavadocField> get() = _fields

    private val _methods: MutableMap<String, JavadocMethod> = hashMapOf()
    val methods: Map<String, JavadocMethod> get() = _methods

    init {
        document.checkJavadocVersion()

        //Get javadoc title
        docTitleElement = JavadocElement.wrap(document.selectFirst("body > div.flex-box > div > main > div > h1"))

        //Get package name
        packageName = document.selectFirst("body > div > div > main > div.header > div.sub-title > a[href='package-summary.html']")?.text() ?: throw DocParseException()

        //Get class name
        className = getClassName(sourceURL)

        //Get class description
        descriptionElements = JavadocElements.fromElements(document.select("#class-description > div.block"))

        //Get class possible's deprecation
        deprecationElement = JavadocElement.tryWrap(document.selectFirst("#class-description > div.deprecation-block"))

        //Get class type parameters if they exist
        val detailTarget = document.selectFirst("#class-description") ?: throw DocParseException()
        detailToElementsMap = DetailToElementsMap.parseDetails(detailTarget)

        //See also
        seeAlso = detailToElementsMap
            .getDetail(DocDetail.Type.SEE_ALSO)
            ?.let { SeeAlso(moduleSession, it) }

        processInheritedElements(moduleSession, document, InheritedType.FIELD, this::onInheritedField)

        processInheritedElements(moduleSession, document, InheritedType.METHOD, this::onInheritedMethod)

        //Try to find field details
        processDetailElements(document, ClassDetailType.FIELD) { fieldElement: Element ->
            val field = JavadocField(this, ClassDetailType.FIELD, fieldElement)
            this._fields[field.elementId] = field
        }

        //Try to find enum constants, they're similar to fields it seems
        processDetailElements(document, ClassDetailType.ENUM_CONSTANTS) { fieldElement: Element ->
            val field = JavadocField(this, ClassDetailType.ENUM_CONSTANTS, fieldElement)
            this._fields[field.elementId] = field
        }

        //Try to find constructor details
        processDetailElements(document, ClassDetailType.CONSTRUCTOR) { methodElement: Element ->
            val method = JavadocMethod(this, ClassDetailType.CONSTRUCTOR, methodElement)
            this._methods[method.elementId] = method
        }

        //Try to find method details
        processDetailElements(document, ClassDetailType.METHOD) { methodElement: Element ->
            val method = JavadocMethod(this, ClassDetailType.METHOD, methodElement)
            this._methods[method.elementId] = method
        }

        //Try to find annotation "methods" (elements)
        processDetailElements(document, ClassDetailType.ANNOTATION_ELEMENT) { annotationElement: Element ->
            val method = JavadocMethod(this, ClassDetailType.ANNOTATION_ELEMENT, annotationElement)
            this._methods[method.elementId] = method
        }
    }

    private fun getClassName(url: String): String =
        url.toHttpUrl().pathSegments.last().dropLast(5) //Remove .html

    @Throws(IOException::class)
    private fun processInheritedElements(
        session: JavadocModuleSession,
        document: Document,
        inheritedType: InheritedType,
        inheritedElementConsumer: (JavadocClass, String?) -> Unit,
    ) {
        val inheritedBlocks = document.select("section." + inheritedType.classSuffix + "-summary > div.inherited-list")
        for (inheritedBlock in inheritedBlocks) {
            val title = inheritedBlock.selectFirst("h3") ?: throw DocParseException()
            val superClassLinkElement = title.selectFirst("a")

            if (superClassLinkElement != null) {
                val superClassLink = superClassLinkElement.absUrl("href")
                val superClassDocs = session.retrieveClassOrNull(superClassLink)
                //Probably a bad link or an unsupported Javadoc version
                if (superClassDocs == null) {
                    if (loggedMissingSuperclassLinks.add(superClassLink))
                        logger.trace { "Skipping superclass at '$superClassLink'" }
                    continue
                }

                for (element in inheritedBlock.select("code > a")) {
                    val targetId = element.absUrl("href").toHttpUrl().fragment
                    inheritedElementConsumer(superClassDocs, targetId)
                }
            }
        }
    }

    private fun onInheritedMethod(superClass: JavadocClass, targetId: String?) {
        // You can inherit the same method multiple times, it will show up multiple times in the docs
        // As the HTML is ordered such as the latest overridden method is shown, we can set the already existing doc to the newest one
        // Example: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/AbstractCollection.html#method.summary
        val method = superClass._methods[targetId]
        // This might happen if the target superclass doesn't expose the same access level of members.
        // For example, this class might expose protected+ members, but the superclass exposes only public members
        if (method == null) return

        _methods[method.elementId] = method
    }

    private fun onInheritedField(superClass: JavadocClass, targetId: String?) {
        val field = superClass._fields[targetId]
        // This might happen if the target superclass doesn't expose the same access level of members.
        // For example, this class might expose protected+ members, but the superclass exposes only public members
        if (field == null) return

        _fields[field.elementId] = field
    }

    private fun processDetailElements(document: Document, detailType: ClassDetailType, callback: (Element) -> Unit) {
        val detailId = detailType.detailId

        //Get main blocks to determine what details are available (field, constructor (constr), method)
        val detailsSection = document.getElementById(detailId) ?: return
        detailsSection.select("ul.member-list > li > section.detail").forEach(callback)
    }

    override fun toString(): String {
        return "%s : %d fields, %d methods%s".format(
            this.className,
            _fields.size,
            _methods.size,
            if (descriptionElements.isEmpty()) "" else " : $descriptionElements"
        )
    }

    override val onlineURL: String?
        get() = moduleSession.source.toOnlineURL(sourceURL)

    val enumConstants: List<JavadocField>
        get() = fields
            .values
            .filter { f: JavadocField -> f.classDetailType == ClassDetailType.ENUM_CONSTANTS }

    val annotationElements: List<JavadocMethod>
        get() = methods
            .values
            .filter { f: JavadocMethod -> f.classDetailType == ClassDetailType.ANNOTATION_ELEMENT }

    private enum class InheritedType(val classSuffix: String) {
        METHOD("method"),
        FIELD("field");
    }
}
