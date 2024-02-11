package com.freya02.docs.data

import com.freya02.bot.utils.HttpUtils
import com.freya02.docs.*
import com.freya02.docs.utils.checkJavadocVersion
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Consumer

class ClassDoc(
    docsSession: DocsSession,
    val sourceURL: String,
    document: Document = HttpUtils.getDocument(sourceURL)
) : BaseDoc() {
    val source: DocSourceType = DocSourceType.fromUrl(sourceURL) ?: throw DocParseException()

    val docTitleElement: HTMLElement
    val classNameFqcn: String by lazy { "$packageName.$className" }
    val packageName: String
    override val className: String
    override val descriptionElements: HTMLElementList
    override val deprecationElement: HTMLElement?

    override val detailToElementsMap: DetailToElementsMap

    override val seeAlso: SeeAlso?

    private val fieldDocs: MutableMap<String, FieldDoc> = hashMapOf()
    private val methodDocs: MutableMap<String, MethodDoc> = hashMapOf()

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
            val fieldDoc = FieldDoc(this, ClassDetailType.FIELD, fieldElement)
            this.fieldDocs[fieldDoc.elementId] = fieldDoc
        }

        //Try to find enum constants, they're similar to fields it seems
        processDetailElements(document, ClassDetailType.ENUM_CONSTANTS) { fieldElement: Element ->
            val fieldDoc = FieldDoc(this, ClassDetailType.ENUM_CONSTANTS, fieldElement)
            this.fieldDocs[fieldDoc.elementId] = fieldDoc
        }

        //Try to find constructor details
        processDetailElements(document, ClassDetailType.CONSTRUCTOR) { methodElement: Element ->
            val methodDoc = MethodDoc(this, ClassDetailType.CONSTRUCTOR, methodElement)
            this.methodDocs[methodDoc.elementId] = methodDoc
        }

        //Try to find method details
        processDetailElements(document, ClassDetailType.METHOD) { methodElement: Element ->
            val methodDoc = MethodDoc(this, ClassDetailType.METHOD, methodElement)
            this.methodDocs[methodDoc.elementId] = methodDoc
        }

        //Try to find annotation "methods" (elements)
        processDetailElements(document, ClassDetailType.ANNOTATION_ELEMENT) { annotationElement: Element ->
            val methodDoc = MethodDoc(this, ClassDetailType.ANNOTATION_ELEMENT, annotationElement)
            this.methodDocs[methodDoc.elementId] = methodDoc
        }
    }

    private fun getClassName(url: String): String =
        url.toHttpUrl().pathSegments.last().dropLast(5) //Remove .html

    @Throws(IOException::class)
    private fun processInheritedElements(
        docsSession: DocsSession,
        document: Document,
        inheritedType: InheritedType,
        inheritedElementConsumer: BiConsumer<ClassDoc, String?>
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

    private fun onInheritedMethod(superClassDocs: ClassDoc, targetId: String?) {
        //You can inherit a same method multiple times, it will show up multiple times in the docs
        // As the html is ordered such as the latest overridden method is shown, we can set the already existing doc to the newest one
        // Example: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/AbstractCollection.html#method.summary
        val methodDoc = superClassDocs.methodDocs[targetId] ?: return

        //This might happen if the target superclass doesn't expose the same access level of members
        // So for example this class might expose protected+ members
        //  but the superclass exposes only public members
        methodDocs[methodDoc.elementId] = methodDoc
    }

    private fun onInheritedField(superClassDocs: ClassDoc, targetId: String?) {
        val fieldDoc = superClassDocs.fieldDocs[targetId] ?: return

        //This might happen if the target superclass doesn't expose the same access level of members
        // So for example this class might expose protected+ members
        //  but the superclass exposes only public members
        fieldDocs[fieldDoc.elementId] = fieldDoc
    }

    fun getFieldDocs(): Map<String, FieldDoc> = Collections.unmodifiableMap(fieldDocs)

    fun getMethodDocs(): Map<String, MethodDoc> = Collections.unmodifiableMap(methodDocs)

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
            fieldDocs.size,
            methodDocs.size,
            if (descriptionElements.isEmpty()) "" else " : " + descriptionElements.toText()
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

    val annotationElements: List<MethodDoc>
        get() = getMethodDocs()
            .values
            .filter { f: MethodDoc -> f.classDetailType == ClassDetailType.ANNOTATION_ELEMENT }
}