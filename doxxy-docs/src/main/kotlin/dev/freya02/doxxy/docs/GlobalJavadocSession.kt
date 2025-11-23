package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.declarations.JavadocClass
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class GlobalJavadocSession(
    val sources: JavadocSources,
) {

    private val moduleSessions = hashMapOf<JavadocSource, JavadocModuleSession>()
    private val lock = ReentrantLock()

    fun retrieveSession(source: JavadocSource): JavadocModuleSession = lock.withLock {
        require(source in sources) {
            "Cannot create a session for an external source"
        }

        moduleSessions.getOrPut(source) {
            JavadocModuleSession(this, source)
        }
    }

    internal fun retrieveClassOrNull(classUrl: DocsURL): JavadocClass? {
        val targetSource = sources.getByUrl(classUrl)
            ?: return null
        val moduleSession = moduleSessions[targetSource]
            ?: return null
        return moduleSession.retrieveClassOrNull(classUrl)
    }
}
