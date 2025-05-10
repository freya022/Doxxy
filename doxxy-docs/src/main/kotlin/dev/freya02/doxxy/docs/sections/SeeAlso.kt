package dev.freya02.doxxy.docs.sections

import dev.freya02.doxxy.docs.DocSourceType
import dev.freya02.doxxy.docs.JavadocModuleSession
import dev.freya02.doxxy.docs.utils.DocUtils
import dev.freya02.doxxy.docs.utils.HttpUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Element

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

    private val references: MutableList<SeeAlsoReference> = ArrayList()

    init {
        for (seeAlsoClassElement in docDetail.htmlElements[0].targetElement.select("dd > ul > li > a")) {
            try {
                // TODO test that we get the expected link when the See Also references another module (JDA -> JDK, for example)
                //  as I'm 99% sure that we need the module session of the target link
                val href = moduleSession.sourceType.toEffectiveURL(seeAlsoClassElement.absUrl("href"))
                val sourceType = DocSourceType.fromUrl(href)
                if (sourceType == null) {
                    tryAddReference(SeeAlsoReference(seeAlsoClassElement.text(), href, TargetType.UNKNOWN, null))
                    continue
                }

                // TODO could be interesting to see if a real workload even crosses sessions
                //  as the previous code only returned a potentially uninitialized ClassDocs
                //  which could happen if, for example, JDA has a JDK link, but the JDK wasn't indexed yet.
                val javadocSession = moduleSession.globalSession.retrieveSession(sourceType)
                //Class should be detectable as all URLs are pulled first
                if (javadocSession.isValidURL(href)) { //TODO check if URLs are checked correctly
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

    private class JavadocUrl private constructor(val className: String, val fragment: String?, val targetType: TargetType) {
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

    companion object {
        private val logger = KotlinLogging.logger { }
    }
}