package dev.freya02.doxxy.bot.docs

import dev.freya02.doxxy.common.Directories
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*

object DocWebServer {
    fun startDocWebServer() {
        embeddedServer(Netty, 25566) {
            routing {
                staticFiles("/", Directories.javadocs.toFile(), index = null)
            }
        }.start()
    }
}