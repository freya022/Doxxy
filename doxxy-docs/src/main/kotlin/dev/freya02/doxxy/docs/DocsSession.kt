package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.declarations.JavadocClass
import dev.freya02.doxxy.docs.utils.DocUtils.isJavadocVersionCorrect
import dev.freya02.doxxy.docs.utils.HttpUtils.removeFragment
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DocsSession {
    private val docMap = hashMapOf<DocsURL, JavadocClass>()
    private val locks = ConcurrentHashMap<DocsURL, ReentrantLock>()

    /**
     * Retrieves the ClassDoc for this URL
     *
     * @param classUrl URL of the class
     * @return `null` if:
     *
     *  - The URL's DocSourceType is not known
     *  - The javadoc version is incorrect
     *
     * Always returns a ClassDoc otherwise
     */
    @Throws(IOException::class)
    fun retrieveDoc(classUrl: DocsURL): JavadocClass? {
        locks.computeIfAbsent(classUrl) { ReentrantLock() }.withLock {
            //Can't use computeIfAbsent as it could be recursively called, throwing a ConcurrentModificationException
            val doc = docMap[classUrl]
            if (doc != null) return doc

            val source = DocSourceType.fromUrl(classUrl) ?: return null

            val document = PageCache[source].getPage(classUrl)
            if (!document.isJavadocVersionCorrect()) return null

            return JavadocClass(this, removeFragment(classUrl), document).also { classDoc ->
                docMap[classUrl] = classDoc
            }
        }
    }
}