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

            val targetSource = session.globalSession.sources.getByUrl(classUrl) ?: return null
            val document = PageCache[targetSource].getPage(classUrl)
            if (!document.isJavadocVersionCorrect()) return null

            return JavadocClass(session, removeFragment(classUrl), document).also { classDoc ->
                docMap[classUrl] = classDoc
            }
        }
    }
}