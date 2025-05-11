package dev.freya02.doxxy.docs.sections

import dev.freya02.doxxy.docs.DocSourceType
import dev.freya02.doxxy.docs.JavadocModuleSession
import dev.freya02.doxxy.docs.utils.DocUtils
import dev.freya02.doxxy.docs.utils.HttpUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Element

private val logger = KotlinLogging.logger { }

class SeeAlso internal constructor(moduleSession: JavadocModuleSession, docDetail: DocDetail) {
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

    private val references = linkedSetOf<SeeAlsoReference>()

    init {
        for (seeAlsoClassElement in docDetail.htmlElements[0].targetElement.select("dd > ul > li > a")) {
            try {
                // Make sure the link is from a known source and is indexed
                val absUrl = seeAlsoClassElement.absUrl("href")
                val targetSourceType = DocSourceType.fromUrl(absUrl)
                if (targetSourceType == null) {
                    references += SeeAlsoReference(seeAlsoClassElement.text(), absUrl, TargetType.UNKNOWN, null)
                    continue
                }

                val javadocSession = moduleSession.globalSession.retrieveSession(targetSourceType)
                val href = targetSourceType.toEffectiveURL(absUrl)
                if (!javadocSession.isValidURL(href)) {
                    references += SeeAlsoReference(seeAlsoClassElement.text(), href, TargetType.UNKNOWN, null)
                    continue
                }

                // Construct an appropriate link from the linked member's type
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

                references += ref
            } catch (e: Exception) {
                logger.error(e) { "An exception occurred while retrieving a 'See also' detail" }
            }
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

    // TODO move SeeAlso ctor to a factory function, then inline this as it will be immutable
    fun getReferences(): Set<SeeAlsoReference> {
        return references
    }

    enum class TargetType(val id: Int) {
        CLASS(1),
        METHOD(2),
        FIELD(3),
        UNKNOWN(-1);

        companion object {
            fun fromFragment(fragment: String?): TargetType {
                return when {
                    fragment == null -> CLASS
                    fragment.contains("(") -> METHOD
                    else -> FIELD
                }
            }

            fun fromId(type: Int): TargetType {
                return entries.find { it.id == type } ?: throw IllegalArgumentException("Unknown type: $type")
            }
        }
    }

    private class JavadocUrl private constructor(
        val className: String,
        val fragment: String?,
        val targetType: TargetType
    ) {
        companion object {
            fun fromURL(url: String): JavadocUrl {
                url.toHttpUrl().let { httpUrl ->
                    val lastFragment = httpUrl.pathSegments.last()
                    require(lastFragment.endsWith(".html"))

                    val className = lastFragment.dropLast(5) //Remove .html
                    val fragment = httpUrl.fragment

                    return JavadocUrl(className, fragment, TargetType.fromFragment(fragment))
                }
            }
        }
    }
}