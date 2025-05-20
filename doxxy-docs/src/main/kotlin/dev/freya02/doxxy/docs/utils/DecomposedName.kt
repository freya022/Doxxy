package dev.freya02.doxxy.docs.utils

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

        fun getDecompositionFromLink(element: Element): DecomposedName {
            require(element.tag().name == "a")
            val href = element.attr("href").ifEmpty { error("href is missing in $element") }
            val title = element.attr("title").ifEmpty { error("title is missing in $element") }

            if (title.startsWith("type parameter"))
                return DecomposedName(null, element.ownText())

            val simpleClassName = href
                .substringAfterLast('/') // Last path segment
                .dropLast(5) // remove .html

            val packageName = title.substringAfterLast(' ')

            return DecomposedName(packageName, simpleClassName)
        }
    }
}