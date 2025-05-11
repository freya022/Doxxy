package dev.freya02.doxxy.docs

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
}
