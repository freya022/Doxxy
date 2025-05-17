package dev.freya02.doxxy.bot.docs

import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import java.nio.file.Path

object DocWebServer {
    suspend fun withLocalJavadocsWebServer(remotePath: String, zipPath: Path, block: suspend () -> Unit) {
        val server = embeddedServer(Netty, 25566) {
            routing {
                staticZip(remotePath, basePath = null, zipPath, index = null)
            }
        }.startSuspend()

        try {
            block()
        } finally {
           server.stopSuspend()
        }
    }
}