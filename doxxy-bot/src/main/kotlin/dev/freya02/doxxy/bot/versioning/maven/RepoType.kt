package dev.freya02.doxxy.bot.versioning.maven

enum class RepoType(val urlFormat: String) {
    MAVEN("https://repo.maven.apache.org/maven2/%s/%s/maven-metadata.xml");
}