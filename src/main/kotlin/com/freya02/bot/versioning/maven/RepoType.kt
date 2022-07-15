package com.freya02.bot.versioning.maven

enum class RepoType(val urlFormat: String) {
    M2("https://m2.dv8tion.net/releases/%s/%s/maven-metadata.xml"),
    MAVEN("https://repo.maven.apache.org/maven2/%s/%s/maven-metadata.xml");
}