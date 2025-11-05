import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    application
    id("com.gradleup.shadow") version "9.2.2"
}

group = "io.github.name" // TODO change here
version = "1.0-SNAPSHOT"

application {
    mainClass = "io.github.name.bot.Main" // TODO change here
}

tasks.withType<ShadowJar> {
    archiveFileName.set("${rootProject.name}.jar")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    //Logging implementation, you can remove this is you use another SLF4J logger
    implementation("ch.qos.logback:logback-classic:1.5.20")

    implementation("%s:%s:%s")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.isIncremental = true

    //JDA supports Java 8 and above
    options.release = 17
}
