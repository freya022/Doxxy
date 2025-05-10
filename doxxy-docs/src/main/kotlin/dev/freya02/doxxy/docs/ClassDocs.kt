package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.exceptions.DocParseException
import dev.freya02.doxxy.docs.utils.DecomposedName
import dev.freya02.doxxy.docs.utils.HttpUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = KotlinLogging.logger { }

class ClassDocs internal constructor(
    private val knownUrls: Set<String>,
    private val constants: Map<FullName, Map<FieldName, FieldValue>>,
    // TODO rename to classUrlMappings?
    val simpleNameToUrlMap: Map<SimpleName, DocsURL>,
) {

    internal fun getConstantsOrNull(fullClassName: FullName): Map<FieldName, FieldValue>? {
        return constants[fullClassName]
    }

    internal fun isValidURL(url: String): Boolean {
        val cleanURL = HttpUtils.removeFragment(url)
        return cleanURL in knownUrls
    }

    companion object {
        private val sourceMap: MutableMap<DocSourceType, ClassDocs> = EnumMap(DocSourceType::class.java)
        private val lock = ReentrantLock()

        /**
         * This is only to be used within code triggered from indexing
         *
         * TODO remove the need for this by passing the ClassDocs instance to every JavadocClass
         *   or use a context parameter for it, as data is likely not accessed outside of constructors/factories
         *   This may also be a good opportunity to merge DocsSession here and rename to JavadocSession/JavadocContext
         */
        fun getSource(source: DocSourceType): ClassDocs {
            return sourceMap[source] ?: error("Source '$source' has not been indexed yet")
        }

        fun getUpdatedSource(source: DocSourceType): ClassDocs = lock.withLock {
            val updatedSource = ClassDocs(source)
            sourceMap[source] = updatedSource
            updatedSource
        }
    }
}

private fun ClassDocs(sourceType: DocSourceType): ClassDocs {
    logger.info { "Parsing ClassDocs URLs for: $sourceType" }

    val classUrlMappings = getClassUrlMappings(sourceType)
    val constants = getConstants(sourceType)
    val knownUrls = classUrlMappings.values.mapTo(hashSetOf()) { sourceType.toEffectiveURL(it) }

    return ClassDocs(
        knownUrls = knownUrls,
        constants = constants,
        simpleNameToUrlMap = classUrlMappings,
    )
}

private fun getClassUrlMappings(sourceType: DocSourceType): Map<SimpleName, DocsURL> = buildMap {
    val indexURL = sourceType.allClassesIndexURL
    val document = PageCache[sourceType].getPage(indexURL)

    //n = 1 needed as type parameters are links and external types
    // For example in AbstractComponentBuilder<T extends AbstractComponentBuilder<T>>
    // It could have selected 4 different URLs, except there is only 1 class we want here
    // Since it's the left most, it's easy to pick the first one
    for (element in document.select("#all-classes-table > div > div.summary-table.two-column-summary > div.col-first > a:nth-child(1)")) {
        val classUrl = element.absUrl("href")
        val (packageName, className) = DecomposedName.getDecompositionFromUrl(sourceType, classUrl)

        if (packageName == null || !sourceType.isValidPackage(packageName)) continue

        val oldUrl = putIfAbsent(className, classUrl)
        if (oldUrl != null) {
            logger.warn { "Detected a duplicate class name '${className}' at '$classUrl' and '$oldUrl'" }
        }
    }
}

private fun getConstants(sourceType: DocSourceType): Map<SimpleName, Map<FieldName, FieldValue>> = buildMap {
    val constantValuesURL = sourceType.constantValuesURL
    val constantsDocument = PageCache[sourceType].getPage(constantValuesURL)

    for (classConstantSection in constantsDocument.select("main section.constants-summary ul.block-list li")) {
        val fullName = run {
            val titleSpan = classConstantSection.selectFirst("div.caption > span")
                ?: throw DocParseException("Expected constant title FQCN name")
            titleSpan.text().substringBefore('<')
        }
        if (fullName in this) {
            logger.warn { "Already got constants for class '$fullName'" }
            continue
        }

        this[fullName] = buildMap {
            val elementIterator = classConstantSection.select("div.summary-table > div").iterator()
            repeat(3) { elementIterator.next() } // Skip headers

            while (elementIterator.hasNext()) {
                elementIterator.next() // "Modifier and type"
                val constantName = elementIterator.next().text()
                val constantValue = elementIterator.next().text()

                this[constantName] = constantValue
            }
        }
    }
}