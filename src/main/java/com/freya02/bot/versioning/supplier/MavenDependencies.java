package com.freya02.bot.versioning.supplier;

import org.intellij.lang.annotations.Language;

public class MavenDependencies implements BuildToolDependencies {
	@Language(value = "xml", prefix = "<project>", suffix = "</project>")
	private static final String BC_XML = """
			<repositories>
			    <repository>
			        <id>jitpack</id>
			        <url>https://jitpack.io</url>
			    </repository>
			</repositories>
			
			<dependencies>
				<dependency>
					<groupId>%s</groupId>
					<artifactId>%s</artifactId>
					<version>%s</version>
				</dependency>
				<dependency>
					<groupId>%s</groupId>
					<artifactId>%s</artifactId>
					<version>%s</version>
				</dependency>
			</dependencies>
			""";

	@Language(value = "xml", prefix = "<project>", suffix = "</project>")
	private static final String JDA4_XML = """
			<repository>
			    <id>dv8tion</id>
			    <name>m2-dv8tion</name>
			    <url>https://m2.dv8tion.net/releases</url>
			</repository>
			
			<dependencies>
				<dependency>
					<groupId>%s</groupId>
					<artifactId>%s</artifactId>
					<version>%s</version>
				</dependency>
			</dependencies>
			""";

	@Language(value = "xml", prefix = "<project>", suffix = "</project>")
	private static final String JDA5_XML = """
			<dependencies>
				<dependency>
					<groupId>%s</groupId>
					<artifactId>%s</artifactId>
					<version>%s</version>
				</dependency>
			</dependencies>
			""";

	@Language(value = "xml", prefix = "<project>", suffix = "</project>")
	private static final String JDA5_JITPACK_XML = """
			<repositories>
				<repository>
			        <id>jitpack</id>
			        <url>https://jitpack.io</url>
			    </repository>
			</repositories>
			
			<dependencies>
				<dependency>
					<groupId>%s</groupId>
					<artifactId>%s</artifactId>
					<version>%s</version>
				</dependency>
			</dependencies>
			""";

	@Override
	public String getBCDependencyFormatString() {
		return BC_XML;
	}

	@Override
	public String getJDA4DependencyFormatString() {
		return JDA4_XML;
	}

	@Override
	public String getJDA5DependencyFormatString() {
		return JDA5_XML;
	}

	@Override
	public String getJDA5JitpackDependencyFormatString() {
		return JDA5_JITPACK_XML;
	}
}
