package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.declarations.JavadocClass
import dev.freya02.doxxy.docs.exceptions.DocParseException
import dev.freya02.doxxy.docs.utils.DecomposedName
import dev.freya02.doxxy.docs.utils.HttpUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

private val logger = KotlinLogging.logger { }

class JavadocModuleSession internal constructor(
    internal val globalSession: GlobalJavadocSession,
    internal val source: JavadocSource,
    private val knownUrls: Set<String>,
    private val constants: Map<FullName, Map<FieldName, FieldValue>>,
    val classUrlMappings: Map<SimpleName, DocsURL>,
) {

    private val cache = JavadocClassCache(this)

    internal fun getConstantsOrNull(fullClassName: FullName): Map<FieldName, FieldValue>? {
        return constants[fullClassName]
    }

    internal fun isValidURL(url: String): Boolean {
        val cleanURL = HttpUtils.removeFragment(url)
        return cleanURL in knownUrls
    }

    fun classesAsFlow(): Flow<JavadocClass> = channelFlow {
        val dispatcher = Dispatchers.IO.limitedParallelism(parallelism = 8, name = "Document fetch")

        classUrlMappings.forEach { (className, classUrl) ->
            launch(dispatcher) {
                try {
                    val javadocClass = retrieveClassOrNull(classUrl) ?: run {
                        logger.warn { "Unable to get docs of '${className}' at '${classUrl}', javadoc version or source type may be incorrect" }
                        return@launch
                    }

                    send(javadocClass)
                } catch (e: Exception) {
                    logger.error(e) { "An exception occurred while reading the docs of '$className' at '$classUrl', skipping." }
                }
            }
        }
    }

    /**
     * Retrieves the [JavadocClass] for this URL
     *
     * @param  classUrl URL of the class
     *
     * @return The [JavadocClass], or `null` if the DocSourceType cannot be determined,
     *         or the Javadoc version is unsupported
     */
    internal fun retrieveClassOrNull(classUrl: DocsURL): JavadocClass? {
        return cache.retrieveClassOrNull(classUrl)
    }
}

internal fun JavadocModuleSession(globalSession: GlobalJavadocSession, source: JavadocSource): JavadocModuleSession {
    logger.info { "Parsing ClassDocs URLs from '${source.sourceUrl}'" }

    val classUrlMappings = getClassUrlMappings(source)
    val constants = getConstants(source)
    val knownUrls = classUrlMappings.values.mapTo(hashSetOf()) { source.toEffectiveURL(it) }

    return JavadocModuleSession(
        globalSession = globalSession,
        source = source,
        knownUrls = knownUrls,
        constants = constants,
        classUrlMappings = classUrlMappings,
    )
}

private fun getClassUrlMappings(source: JavadocSource): Map<SimpleName, DocsURL> = buildMap {
    val indexURL = source.allClassesIndexURL
    val document = PageCache[source].getPage(indexURL)

    //n = 1 needed as type parameters are links and external types
    // For example in AbstractComponentBuilder<T extends AbstractComponentBuilder<T>>
    // It could have selected 4 different URLs, except there is only 1 class we want here
    // Since it's the left most, it's easy to pick the first one
    for (element in document.select("#all-classes-table > div > div.summary-table.two-column-summary > div.col-first > a:nth-child(1)")) {
        val classUrl = element.absUrl("href")
        val (packageName, className) = DecomposedName.getDecompositionFromLink(element)

        if (packageName == null || !source.isValidPackage(packageName)) continue

        val oldUrl = putIfAbsent(className, classUrl)
        if (oldUrl != null) {
            logger.warn { "Detected a duplicate class name '${className}' at '$classUrl' and '$oldUrl'" }
        }
    }
}

private fun getConstants(source: JavadocSource): Map<SimpleName, Map<FieldName, FieldValue>> = buildMap {
    val constantValuesURL = source.constantValuesURL
    val constantsDocument = PageCache[source].getPage(constantValuesURL)

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