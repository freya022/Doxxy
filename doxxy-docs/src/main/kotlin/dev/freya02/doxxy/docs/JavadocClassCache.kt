package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.declarations.JavadocClass
import dev.freya02.doxxy.docs.utils.DocUtils.isJavadocVersionCorrect
import dev.freya02.doxxy.docs.utils.HttpUtils.removeFragment
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class JavadocClassCache internal constructor(
    private val session: JavadocModuleSession,
) {

    // TODO is this actually doing anything..?
    private val pageCache = PageCache[session.source]

    private val docMap = hashMapOf<DocsURL, JavadocClass>()
    private val locks = ConcurrentHashMap<DocsURL, ReentrantLock>()

    private val currentUrls: ThreadLocal<Set<DocsURL>> = ThreadLocal.withInitial { hashSetOf() }

    internal fun retrieveClassOrNull(classUrl: DocsURL): JavadocClass? {
        docMap[classUrl]?.let { return it }

        check(classUrl !in currentUrls.get()) {
            "Recursion detected for $classUrl in ${currentUrls.get()}"
        }

        locks.computeIfAbsent(classUrl) { ReentrantLock() }.withLock {
            docMap[classUrl]?.let { return it }

            // TODO this part should probably be moved in a closure in JavadocModuleSession
            val requestedSource = session.globalSession.sources.getByUrl(classUrl)
            require(requestedSource == session.source) {
                "Class '$classUrl' is from a different source (current: ${session.source}, requested: $requestedSource)"
            }

            val document = pageCache.getPage(classUrl)
            if (!document.isJavadocVersionCorrect()) return null

            return JavadocClass(session, removeFragment(classUrl), document).also { javadocClass ->
                docMap[classUrl] = javadocClass
            }
        }
    }
}
