package com.freya02.docs

import com.freya02.bot.utils.HttpUtils.removeFragment
import com.freya02.docs.DocUtils.isJavadocVersionCorrect
import com.freya02.docs.data.ClassDoc
import com.freya02.docs.utils.DocsURL
import java.io.IOException

class DocsSession {
    private val docMap: MutableMap<DocsURL, ClassDoc> = HashMap()

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
    fun retrieveDoc(classUrl: DocsURL): ClassDoc? {
        synchronized(docMap) {
            //Can't use computeIfAbsent as it could be recursively called, throwing a ConcurrentModificationException
            val doc = docMap[classUrl]

            if (doc == null) {
                val source = DocSourceType.fromUrl(classUrl) ?: return null

                val document = PageCache[source].getPage(classUrl)
                if (!document.isJavadocVersionCorrect()) return null

                return ClassDoc(this, removeFragment(classUrl), document).also { classDoc ->
                    docMap[classUrl] = classDoc
                }
            }

            return doc
        }
    }
}