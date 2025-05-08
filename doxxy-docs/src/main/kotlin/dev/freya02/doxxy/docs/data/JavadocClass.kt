package dev.freya02.doxxy.docs.data

import dev.freya02.doxxy.docs.*
import dev.freya02.doxxy.docs.utils.HttpUtils
import dev.freya02.doxxy.docs.utils.checkJavadocVersion
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Consumer

class JavadocClass(
    docsSession: DocsSession,
    val sourceURL: String,
    document: Document = HttpUtils.getDocument(sourceURL)
) : AbstractJavadoc() {
    val source: DocSourceType = DocSourceType.fromUrl(sourceURL) ?: throw DocParseException()

    val docTitleElement: HTMLElement
    val classNameFqcn: String by lazy { "$packageName.$className" }
    val packageName: String
    override val className: String
    override val descriptionElements: HTMLElementList
    override val deprecationElement: HTMLElement?

    override val detailToElementsMap: DetailToElementsMap

    override val seeAlso: SeeAlso?

    private val fields: MutableMap<String, FieldDoc> = hashMapOf()
    private val methods: MutableMap<String, JavadocMethod> = hashMapOf()

    init {
        document.checkJavadocVersion()

        //Get javadoc title
        docTitleElement = HTMLElement.wrap(document.selectFirst("body > div.flex-box > div > main > div > h1"))

        //Get package name
        packageName = document.selectFirst("body > div > div > main > div.header > div.sub-title > a[href='package-summary.html']")?.text() ?: throw DocParseException()

        //Get class name
        className = getClassName(sourceURL)

        //Get class description
        descriptionElements = HTMLElementList.fromElements(document.select("#class-description > div.block"))

        //Get class possible's deprecation
        deprecationElement = HTMLElement.tryWrap(document.selectFirst("#class-description > div.deprecation-block"))

        //Get class type parameters if they exist
        val detailTarget = document.selectFirst("#class-description") ?: throw DocParseException()
        detailToElementsMap = DetailToElementsMap.parseDetails(detailTarget)

        //See also
        val seeAlsoDetail = detailToElementsMap.getDetail(DocDetailType.SEE_ALSO)
        seeAlso = when {
            seeAlsoDetail != null -> SeeAlso(source, seeAlsoDetail)
            else -> null
        }

        processInheritedElements(docsSession, document, InheritedType.FIELD, this::onInheritedField)

        processInheritedElements(docsSession, document, InheritedType.METHOD, this::onInheritedMethod)

        //Try to find field details
        processDetailElements(document, ClassDetailType.FIELD) { fieldElement: Element ->
            val field = FieldDoc(this, ClassDetailType.FIELD, fieldElement)
            this.fields[field.elementId] = field
        }

        //Try to find enum constants, they're similar to fields it seems
        processDetailElements(document, ClassDetailType.ENUM_CONSTANTS) { fieldElement: Element ->
            val field = FieldDoc(this, ClassDetailType.ENUM_CONSTANTS, fieldElement)
            this.fields[field.elementId] = field
        }

        //Try to find constructor details
        processDetailElements(document, ClassDetailType.CONSTRUCTOR) { methodElement: Element ->
            val method = JavadocMethod(this, ClassDetailType.CONSTRUCTOR, methodElement)
            this.methods[method.elementId] = method
        }

        //Try to find method details
        processDetailElements(document, ClassDetailType.METHOD) { methodElement: Element ->
            val method = JavadocMethod(this, ClassDetailType.METHOD, methodElement)
            this.methods[method.elementId] = method
        }

        //Try to find annotation "methods" (elements)
        processDetailElements(document, ClassDetailType.ANNOTATION_ELEMENT) { annotationElement: Element ->
            val method = JavadocMethod(this, ClassDetailType.ANNOTATION_ELEMENT, annotationElement)
            this.methods[method.elementId] = method
        }
    }

    private fun getClassName(url: String): String =
        url.toHttpUrl().pathSegments.last().dropLast(5) //Remove .html

    @Throws(IOException::class)
    private fun processInheritedElements(
        docsSession: DocsSession,
        document: Document,
        inheritedType: InheritedType,
        inheritedElementConsumer: BiConsumer<JavadocClass, String?>
    ) {
        val inheritedBlocks = document.select("section." + inheritedType.classSuffix + "-summary > div.inherited-list")
        for (inheritedBlock in inheritedBlocks) {
            val title = inheritedBlock.selectFirst("h3") ?: throw DocParseException()
            val superClassLinkElement = title.selectFirst("a")

            if (superClassLinkElement != null) {
                val superClassLink = superClassLinkElement.absUrl("href")
                val superClassDocs = docsSession.retrieveDoc(superClassLink) ?: continue

                //Probably a bad link or an unsupported javadoc version
                for (element in inheritedBlock.select("code > a")) {
                    val targetId = element.absUrl("href").toHttpUrl().fragment
                    inheritedElementConsumer.accept(superClassDocs, targetId)
                }
            }
        }
    }

    private fun onInheritedMethod(superClass: JavadocClass, targetId: String?) {
        //You can inherit a same method multiple times, it will show up multiple times in the docs
        // As the html is ordered such as the latest overridden method is shown, we can set the already existing doc to the newest one
        // Example: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/AbstractCollection.html#method.summary
        val method = superClass.methods[targetId] ?: return

        //This might happen if the target superclass doesn't expose the same access level of members
        // So for example this class might expose protected+ members
        //  but the superclass exposes only public members
        methods[method.elementId] = method
    }

    private fun onInheritedField(superClass: JavadocClass, targetId: String?) {
        val field = superClass.fields[targetId] ?: return

        //This might happen if the target superclass doesn't expose the same access level of members
        // So for example this class might expose protected+ members
        //  but the superclass exposes only public members
        fields[field.elementId] = field
    }

    fun getFieldDocs(): Map<String, FieldDoc> = Collections.unmodifiableMap(fields)

    fun getMethodDocs(): Map<String, JavadocMethod> = Collections.unmodifiableMap(methods)

    private fun processDetailElements(document: Document, detailType: ClassDetailType, callback: Consumer<Element>) {
        val detailId = detailType.detailId

        //Get main blocks to determine what details are available (field, constructor (constr), method)
        val detailsSection = document.getElementById(detailId) ?: return
        for (element in detailsSection.select("ul.member-list > li > section.detail")) {
            callback.accept(element)
        }
    }

    override fun toString(): String {
        return "%s : %d fields, %d methods%s".format(
            this.className,
            fields.size,
            methods.size,
            if (descriptionElements.isEmpty()) "" else " : $descriptionElements"
        )
    }

    override val effectiveURL: String
        get() = source.toEffectiveURL(sourceURL)
    override val onlineURL: String?
        get() = source.toOnlineURL(sourceURL)

    override val identifier: String? = null
    override val identifierNoArgs: String? = null
    override val humanIdentifier: String? = null
    override fun toHumanClassIdentifier(className: String): String? = null
    override val returnType: String? = null

    val enumConstants: List<FieldDoc>
        get() = getFieldDocs()
            .values
            .filter { f: FieldDoc -> f.classDetailType == ClassDetailType.ENUM_CONSTANTS }

    val annotationElements: List<JavadocMethod>
        get() = getMethodDocs()
            .values
            .filter { f: JavadocMethod -> f.classDetailType == ClassDetailType.ANNOTATION_ELEMENT }
}