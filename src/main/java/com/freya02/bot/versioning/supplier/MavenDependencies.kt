package com.freya02.bot.versioning.supplier

import org.intellij.lang.annotations.Language

object MavenDependencies : BuildToolDependencies {
    @Language(value = "xml", prefix = "<project>", suffix = "</project>")
    override val bcDependencyFormatString: String = """
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
    """.trimIndent()

    @Language(value = "xml", prefix = "<project>", suffix = "</project>")
    override val jda4DependencyFormatString: String = """
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
    """.trimIndent()

    @Language(value = "xml", prefix = "<project>", suffix = "</project>")
    override val jda5DependencyFormatString: String = """
        <dependencies>
            <dependency>
                <groupId>%s</groupId>
                <artifactId>%s</artifactId>
                <version>%s</version>
            </dependency>
        </dependencies>
    """.trimIndent()

    @Language(value = "xml", prefix = "<project>", suffix = "</project>")
    override val jdaJitpackDependencyFormatString: String = """
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
    """.trimIndent()
}