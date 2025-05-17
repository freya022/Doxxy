package dev.freya02.doxxy.docs

import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import kotlin.io.path.*
import kotlin.test.Test
import kotlin.test.assertEquals

object IntegrationTests {

    private val sources = JavadocSources(listOf(JDA_SOURCE, JDK_SOURCE))

    @BeforeAll
    @JvmStatic
    fun setup() {
        PageCache[JDA_SOURCE].clearCache()

        embeddedServer(Netty, host = JDA_HOST, port = JDA_PORT) {
            routing {
                staticZip(JDA_PATH, "", getResourcePath("/JDA-javadoc.zip"), index = null)
            }
        }.start()
    }

    @Test
    fun `Read JDA Javadocs`() {
        val globalSession = GlobalJavadocSession(sources)
        val moduleSession = globalSession.retrieveSession(JDA_SOURCE)

        val classes = runBlocking {
            moduleSession
                .classesAsFlow()
                .buffer()
                .toList()
        }

        val actualSnapshot = classes.map { it.classNameFqcn }.sorted().joinToString("\n")
        val snapshotPath = Path("snapshots", "JDA.txt")
        if (snapshotPath.exists()) {
            val expectedSnapshot = snapshotPath.readText()
            assertEquals(expectedSnapshot, actualSnapshot)
        } else {
            snapshotPath.createParentDirectories().writeText(actualSnapshot)
        }
    }
}