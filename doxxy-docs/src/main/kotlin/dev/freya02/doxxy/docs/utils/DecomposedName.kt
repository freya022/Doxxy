package dev.freya02.doxxy.docs.utils

import dev.freya02.doxxy.docs.JavadocSource
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jetbrains.annotations.Contract
import org.jsoup.nodes.Element

@JvmRecord
internal data class DecomposedName(val packageName: String?, val className: String) {
    companion object {
        fun getSimpleClassName(fullName: String): String {
            for (i in 0 until fullName.length - 1) {
                if (fullName[i] == '.' && Character.isUpperCase(fullName[i + 1])) {
                    return fullName.substring(i + 1)
                }
            }

            //No name if naming conventions aren't respected
            return fullName
        }

        // TODO replace with getDecompositionFromLink
        @Contract("_, _ -> new")
        fun getDecompositionFromUrl(source: JavadocSource, target: String): DecomposedName {
            val sourceUrl = source.sourceUrl.toHttpUrl()
            val targetUrl = target.toHttpUrl()
            val rightSegments: MutableList<String> =
                ArrayList(targetUrl.pathSegments.subList(sourceUrl.pathSize, targetUrl.pathSize))

            //Remove java 9 modules from segments
            if (rightSegments[0].startsWith("java.")) rightSegments.removeAt(0)

            //All segments except last
            val packageSegments = rightSegments.subList(0, rightSegments.size - 1)
            val lastSegment = rightSegments.last()

            //Remove .html extension
            val className = lastSegment.substringBeforeLast('.')
            return DecomposedName(packageSegments.joinToString("."), className)
        }

        fun getDecompositionFromLink(element: Element): DecomposedName {
            require(element.tag().name == "a")
            val href = element.attr("href").ifEmpty { error("href is missing in $element") }
            val title = element.attr("title").ifEmpty { error("title is missing in $element") }

            val simpleClassName = href
                .substringAfterLast('/') // Last path segment
                .dropLast(5) // remove .html

            val packageName = title.substringAfterLast(' ')

            return DecomposedName(packageName, simpleClassName)
        }
    }
}