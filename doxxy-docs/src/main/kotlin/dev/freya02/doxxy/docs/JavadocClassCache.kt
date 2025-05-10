package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.declarations.JavadocClass
import dev.freya02.doxxy.docs.utils.DocUtils.isJavadocVersionCorrect
import dev.freya02.doxxy.docs.utils.HttpUtils.removeFragment
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class JavadocClassCache internal constructor(
    private val session: ClassDocs,
) {

    private val docMap = hashMapOf<DocsURL, JavadocClass>()
    private val locks = ConcurrentHashMap<DocsURL, ReentrantLock>()

    fun retrieveClassOrNull(classUrl: DocsURL): JavadocClass? {
        locks.computeIfAbsent(classUrl) { ReentrantLock() }.withLock {
            //Can't use computeIfAbsent as it could be recursively called, throwing a ConcurrentModificationException
            val doc = docMap[classUrl]
            if (doc != null) return doc

            val source = DocSourceType.fromUrl(classUrl) ?: return null

            val document = PageCache[source].getPage(classUrl)
            if (!document.isJavadocVersionCorrect()) return null

            return JavadocClass(session, removeFragment(classUrl), document).also { classDoc ->
                docMap[classUrl] = classDoc
            }
        }
    }
}