package dev.freya02.doxxy.docs

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import dev.freya02.doxxy.bot.config.Data
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.bufferedReader
import kotlin.io.path.exists

object DocWebServer {
    private val logger = KotlinLogging.logger { }

    @Throws(IOException::class)
    fun startDocWebServer() {
        val server = HttpServer.create(InetSocketAddress(25566), 0)
        server.createContext("/") { exchange: HttpExchange ->
            if (!exchange.remoteAddress.address.isLoopbackAddress) {
                logger.warn { "A non-loopback address tried to connect: ${exchange.remoteAddress}" }
                return@createContext
            }

            val path = exchange.requestURI.path
            val javadocsPath = Data.javadocsPath
            val file = javadocsPath.resolve(path.substring(1))
            if (!file.startsWith(javadocsPath)) {
                return@createContext logger.warn { "Tried to access a file outside of the target directory: '$file', from ${exchange.remoteAddress}" }
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