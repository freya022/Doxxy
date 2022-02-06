package com.freya02.bot.versioning;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;

public class VersionsUtils {
	public static void extractZip(Path tempZip, Path targetDocsFolder) throws IOException {
		if (Files.exists(targetDocsFolder)) {
			for (Path path : Files.walk(targetDocsFolder).sorted(Comparator.reverseOrder()).toList()) {
				Files.deleteIfExists(path);
			}
		}

		try (FileSystem zfs = FileSystems.newFileSystem(tempZip)) {
			final Path zfsRoot = zfs.getPath("/");

			for (Path sourcePath : Files.walk(zfsRoot)
					.filter(Files::isRegularFile)
					.filter(p -> p.getFileName().toString().endsWith("html"))
					.toList()) {
				final Path targetPath = targetDocsFolder.resolve(zfsRoot.relativize(sourcePath).toString());

				Files.createDirectories(targetPath.getParent());
				Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}
}
