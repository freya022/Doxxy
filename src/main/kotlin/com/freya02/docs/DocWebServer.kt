package com.freya02.docs

import com.freya02.bot.Main
import com.freya02.botcommands.api.Logging
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.bufferedReader
import kotlin.io.path.exists

private val LOGGER = Logging.getLogger()

object DocWebServer {
    @Throws(IOException::class)
    fun startDocWebServer() {
        val server = HttpServer.create(InetSocketAddress(25566), 0)
        server.createContext("/") { exchange: HttpExchange ->
            if (!exchange.remoteAddress.address.isLoopbackAddress) {
                LOGGER.warn("A non-loopback address tried to connect: {}", exchange.remoteAddress)
                return@createContext
            }

            val path = exchange.requestURI.path
            val javadocsPath = Main.JAVADOCS_PATH
            val file = javadocsPath.resolve(path.substring(1))
            if (!file.startsWith(javadocsPath)) {
                LOGGER.warn(
                    "Tried to access a file outside of the target directory: '{}', from {}",
                    file,
                    exchange.remoteAddress
                )
                return@createContext
            }

            exchange.responseBody.bufferedWriter().use { writer ->
                if (file.exists()) {
                    val attributes = Files.readAttributes(file, BasicFileAttributes::class.java)

                    if (attributes.isRegularFile) {
                        exchange.sendResponseHeaders(200, attributes.size())
                        file.bufferedReader().use { reader -> reader.transferTo(writer) }

                        return@createContext
                    }
                }

                exchange.sendResponseHeaders(404, "404 (Not Found)".length.toLong())
                writer.write("404 (Not Found)")
            }
        }

        server.start()
    }
}