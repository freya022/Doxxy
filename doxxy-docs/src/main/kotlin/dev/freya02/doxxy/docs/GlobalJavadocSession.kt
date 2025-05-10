package dev.freya02.doxxy.docs

import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class GlobalJavadocSession(

) {

    private val moduleSessions = EnumMap<_, JavadocModuleSession>(DocSourceType::class.java)
    private val lock = ReentrantLock()

    fun retrieveSession(sourceType: DocSourceType): JavadocModuleSession = lock.withLock {
        moduleSessions.getOrPut(sourceType) {
            JavadocModuleSession(this, sourceType)
        }
    }
}
