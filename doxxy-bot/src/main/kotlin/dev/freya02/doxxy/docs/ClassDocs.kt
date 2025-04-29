package dev.freya02.doxxy.docs

import dev.freya02.doxxy.bot.utils.DecomposedName
import dev.freya02.doxxy.bot.utils.HttpUtils
import dev.freya02.doxxy.docs.utils.DocsURL
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.util.*

// TODO Move javadoc parsing to module, try to internalize stuff
class ClassDocs private constructor(private val source: DocSourceType) {
    private val simpleNameToUrlMap: MutableMap<String, DocsURL> = HashMap()
    private val urlSet: MutableSet<String> = HashSet()
    private val fqcnToConstantsMap: MutableMap<String, MutableMap<String, String>> = hashMapOf()

    fun getSimpleNameToUrlMap(): Map<String, DocsURL> {
        return simpleNameToUrlMap
    }

    fun getFqcnToConstantsMap(): Map<String, Map<String, String>> = fqcnToConstantsMap

    fun isValidURL(url: String?): Boolean {
        val cleanURL = HttpUtils.removeFragment(url!!)
        return urlSet.contains(cleanURL)
    }

    @Synchronized
    @Throws(IOException::class)
    private fun tryIndexAll() {
        val indexURL = source.allClassesIndexURL
        val constantValuesURL = source.constantValuesURL

        logger.info { "Parsing ClassDocs URLs for: $source" }
        val document = PageCache[source].getPage(indexURL)
        val constantsDocument = PageCache[source].getPage(constantValuesURL)

        simpleNameToUrlMap.clear()
        urlSet.clear()

        //n = 1 needed as type parameters are links and external types
        // For example in AbstractComponentBuilder<T extends AbstractComponentBuilder<T>>
        // It could have selected 4 different URLs, except there is only 1 class we want here
        // Since it's the left most, it's easy to pick the first one
        for (element in document.select("#all-classes-table > div > div.summary-table.two-column-summary > div.col-first > a:nth-child(1)")) {
            val classUrl = element.absUrl("href")
            val decomposition = DecomposedName.getDecompositionFromUrl(source, classUrl)

            if (!source.isValidPackage(decomposition.packageName)) continue

            val oldUrl = simpleNameToUrlMap.put(decomposition.className, classUrl)
            if (oldUrl != null) {
                logger.warn { "Detected a duplicate class name '${decomposition.className}' at '$classUrl' and '$oldUrl'" }
            } else {
                urlSet.add(source.toEffectiveURL(classUrl)) //For quick checks
            }
        }

        for (classConstantSection in constantsDocument.select("main section.constants-summary ul.block-list li")) {
            val titleSpan = classConstantSection.selectFirst("div.caption > span") ?: throw DocParseException("Expected constant title FQCN name")
            val map = fqcnToConstantsMap.getOrPut(titleSpan.text().substringBefore('<')) { hashMapOf() }

            //Constant values document wasn't modernized so the html layout sucks.
            val elements = classConstantSection.select("div.summary-table > div").drop(3).toMutableList()
            while (elements.isNotEmpty()) {
                elements.removeFirst() //Modifier & type
                val constantName = elements.removeFirst().text()
                val constantValue = elements.removeFirst().text()

                map[constantName] = constantValue
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger { }

        private val sourceMap: MutableMap<DocSourceType, ClassDocs> = EnumMap(DocSourceType::class.java)

        @Synchronized
        fun getSource(source: DocSourceType): ClassDocs {
            return sourceMap.computeIfAbsent(source) { ClassDocs(source) }
        }

        @Synchronized
        @Throws(IOException::class)
        fun getUpdatedSource(source: DocSourceType): ClassDocs {
            return sourceMap
                .computeIfAbsent(source) { ClassDocs(source) }
                .also { it.tryIndexAll() }
        }
    }
}