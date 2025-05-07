package dev.freya02.doxxy.docs.utils

import dev.freya02.doxxy.docs.DocSourceType
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jetbrains.annotations.Contract

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

        fun getPackageName(fullName: String): String? {
            for (i in 0 until fullName.length - 1) {
                if (fullName[i] == '.' && Character.isUpperCase(fullName[i + 1])) {
                    return fullName.substring(0, i)
                }
            }

            //No package if naming conventions aren't respected
            return null
        }

        @Contract("_ -> new")
        fun getDecomposition(fullName: String): DecomposedName {
            for (i in 0 until fullName.length - 1) {
                if (fullName[i] == '.' && Character.isUpperCase(fullName[i + 1])) {
                    return DecomposedName(fullName.substring(0, i), fullName.substring(i + 1))
                }
            }

            //No package if naming conventions aren't respected
            return DecomposedName(null, fullName)
        }

        @Contract("_, _ -> new")
        fun getDecompositionFromUrl(sourceType: DocSourceType, target: String): DecomposedName {
            val sourceUrl = sourceType.sourceUrl.toHttpUrl()
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
    }
}