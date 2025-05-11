package dev.freya02.doxxy.docs

import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.BeforeAll
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals

object IntegrationTests {

    @BeforeAll
    @JvmStatic
    fun setup() {
        PageCache[DocSourceType.JDA].clearCache()

        embeddedServer(Netty, host = "localhost", port = DocSourceType.JDA.sourceUrl.toHttpUrl().port) {
            routing {
                staticZip("/JDA", "", Path("test-files", "JDA-javadoc.zip"), index = null)
            }
        }.start()
    }

    @Test
    fun `Read JDA Javadocs`() {
        val globalSession = GlobalJavadocSession()
        val moduleSession = globalSession.retrieveSession(DocSourceType.JDA)

        val classes = runBlocking {
            moduleSession
                .classesAsFlow()
                .buffer()
                .toList()
        }

        assertEquals(768, classes.size)
    }
}