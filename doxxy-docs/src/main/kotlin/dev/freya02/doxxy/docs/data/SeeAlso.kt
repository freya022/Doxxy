package dev.freya02.doxxy.docs.data

import dev.freya02.doxxy.docs.ClassDocs
import dev.freya02.doxxy.docs.DocSourceType
import dev.freya02.doxxy.docs.DocUtils
import dev.freya02.doxxy.docs.JavadocUrl
import dev.freya02.doxxy.docs.utils.HttpUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Element

class SeeAlso(type: DocSourceType, docDetail: DocDetail) {
    class SeeAlsoReference(
        val text: String,
        val link: String,
        val targetType: TargetType,
        val fullSignature: String?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val that = other as SeeAlsoReference
            return if (targetType !== that.targetType) false else {
                if (fullSignature == that.fullSignature && fullSignature == null) {
                    HttpUtils.removeFragment(link) == HttpUtils.removeFragment(that.link)
                } else fullSignature == that.fullSignature
            }
        }

        override fun hashCode(): Int {
            var result = targetType.hashCode()
            result = 31 * result + (fullSignature?.hashCode() ?: 0)
            return result
        }
    }

    private val references: MutableList<SeeAlsoReference> = ArrayList()

    init {
        for (seeAlsoClassElement in docDetail.htmlElements[0].targetElement.select("dd > ul > li > a")) {
            try {
                val href = type.toEffectiveURL(seeAlsoClassElement.absUrl("href"))
                val sourceType = DocSourceType.fromUrl(href)
                if (sourceType == null) {
                    tryAddReference(SeeAlsoReference(seeAlsoClassElement.text(), href, TargetType.UNKNOWN, null))
                    continue
                }

                val classDocs = ClassDocs.getSource(sourceType)
                //Class should be detectable as all URLs are pulled first
                if (classDocs.isValidURL(href)) { //TODO check if URLs are checked correctly
                    //Class exists
                    //Is it a class, method, or field
                    val javadocUrl = JavadocUrl.fromURL(href)
                    val className = javadocUrl.className
                    val targetAsText = getTargetAsText(seeAlsoClassElement, javadocUrl.targetType)

                    val ref = when (javadocUrl.targetType) {
                        TargetType.CLASS -> SeeAlsoReference(targetAsText, href, TargetType.CLASS, className)
                        TargetType.METHOD -> SeeAlsoReference(
                            targetAsText,
                            href,
                            TargetType.METHOD,
                            className + "#" + DocUtils.getSimpleSignature(javadocUrl.fragment!!)
                        )
                        TargetType.FIELD -> SeeAlsoReference(
                            targetAsText,
                            href,
                            TargetType.FIELD,
                            className + "#" + javadocUrl.fragment!!
                        )
                        else -> throw IllegalStateException("Unexpected javadoc target type: " + javadocUrl.targetType)
                    }

                    tryAddReference(ref)
                } else {
                    tryAddReference(SeeAlsoReference(seeAlsoClassElement.text(), href, TargetType.UNKNOWN, null))
                }
            } catch (e: Exception) {
                logger.error(e) { "An exception occurred while retrieving a 'See also' detail" }
            }
        }
    }

    private fun tryAddReference(seeAlsoClassElement: SeeAlsoReference) {
        if (!references.contains(seeAlsoClassElement)) {
            references.add(seeAlsoClassElement)
        }
    }

    private fun getTargetAsText(seeAlsoClassElement: Element, targetType: TargetType): String {
        val text = seeAlsoClassElement.text()
        if (targetType !== TargetType.METHOD) return text

        val textBuilder = StringBuilder(text)
        val parenthesisIndex = text.indexOf('(')
        val index = text.lastIndexOf('.', if (parenthesisIndex == -1) 0 else parenthesisIndex)
        if (index > -1) {
            textBuilder.replace(index, index + 1, "#")
        }

        return textBuilder.toString()
    }

    fun getReferences(): List<SeeAlsoReference> {
        return references
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}