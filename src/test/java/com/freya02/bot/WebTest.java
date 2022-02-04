package com.freya02.bot;

import com.freya02.botcommands.api.Logging;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class WebTest {
	private static final Logger LOGGER = Logging.getLogger();

	public static void main(String[] args) throws IOException {
		final HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

		server.createContext("/", exchange -> {
			if (!exchange.getRemoteAddress().getAddress().isLoopbackAddress()) {
				LOGGER.warn("A non-loopback address tried to connect: {}", exchange.getRemoteAddress());

				return;
			}

			final String path = exchange.getRequestURI().getPath();

			final Path javadocsPath = Main.BOT_FOLDER.resolve("javadocs");
			final Path file = javadocsPath.resolve(path.substring(1));

			if (!file.startsWith(javadocsPath)) {
				LOGGER.warn("Tried to access a file outside of the target directory: '{}', from {}", file, exchange.getRemoteAddress());

				return;
			}

			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(exchange.getResponseBody()))) {
				if (Files.exists(file)) {
					final BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);

					if (attributes.isRegularFile()) {
						exchange.sendResponseHeaders(200, attributes.size());

						try (BufferedReader reader = Files.newBufferedReader(file)) {
							reader.transferTo(writer);
						}

						return;
					}
				}

				exchange.sendResponseHeaders(404, "404 (Not Found)".length());

				writer.write("404 (Not Found)");
			}
		});

		server.start();
	}
}
