package dev.freya02.doxxy.docs.sections

import dev.freya02.doxxy.docs.JavadocModuleSession
import dev.freya02.doxxy.docs.sections.SeeAlso.SeeAlsoReference
import dev.freya02.doxxy.docs.sections.SeeAlso.TargetType
import dev.freya02.doxxy.docs.utils.DocUtils
import dev.freya02.doxxy.docs.utils.HttpUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Element

private val logger = KotlinLogging.logger { }

class SeeAlso internal constructor(
    val references: Set<SeeAlsoReference>,
) {
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
}

internal fun SeeAlso(moduleSession: JavadocModuleSession, docDetail: DocDetail): SeeAlso {
    val references = docDetail[0].targetElement
        .select("dd > ul > li > a")
        .mapNotNullTo(linkedSetOf()) { seeAlsoClassElement ->
            try {
                // Make sure the link is from a known source and is indexed
                val absUrl = seeAlsoClassElement.absUrl("href")
                val targetSourceType = moduleSession.globalSession.sources.getByUrl(absUrl)
                if (targetSourceType == null) {
                    return@mapNotNullTo SeeAlsoReference(seeAlsoClassElement.text(), absUrl, TargetType.UNKNOWN, null)
                }

                val javadocSession = moduleSession.globalSession.retrieveSession(targetSourceType)
                val href = targetSourceType.toEffectiveURL(absUrl)
                if (!javadocSession.isValidURL(href)) {
                    return@mapNotNullTo SeeAlsoReference(seeAlsoClassElement.text(), href, TargetType.UNKNOWN, null)
                }

                // Construct an appropriate link from the linked member's type
                val javadocUrl = JavadocUrl.fromURL(href)
                val className = javadocUrl.className
                val targetAsText = getTargetAsText(seeAlsoClassElement, javadocUrl.targetType)

                when (javadocUrl.targetType) {
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
            } catch (e: Exception) {
                logger.error(e) { "An exception occurred while retrieving a 'See also' detail" }
                null
            }
        }

    return SeeAlso(references)
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