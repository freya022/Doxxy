package com.freya02.bot.commands.slash.versioning;

import com.freya02.bot.versioning.ArtifactInfo;
import org.intellij.lang.annotations.Language;

public class VersioningCommons {
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

	public static String formatBC(ArtifactInfo jdaVersionFromBotCommands, ArtifactInfo latestBotCommands) {
		return BC_XML.formatted(jdaVersionFromBotCommands.groupId(), jdaVersionFromBotCommands.artifactId(), jdaVersionFromBotCommands.version(),
				latestBotCommands.groupId(), latestBotCommands.artifactId(), latestBotCommands.version());
	}

	public static String formatJDA5(ArtifactInfo latestJDAVersion) {
		return JDA5_XML.formatted(latestJDAVersion.groupId(), latestJDAVersion.artifactId(), latestJDAVersion.version());
	}

	public static String formatJDA4(ArtifactInfo latestJDAVersion) {
		return JDA4_XML.formatted(latestJDAVersion.groupId(), latestJDAVersion.artifactId(), latestJDAVersion.version());
	}
}
