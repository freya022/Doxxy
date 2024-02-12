import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

application.mainClass.set("io.github.name.bot.Main")    //TODO change here
group = "io.github.name"                                //TODO change here
version = "1.0-SNAPSHOT"

tasks.withType<ShadowJar> {
    archiveFileName.set("your-project-name.jar")        //TODO change here
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    //Logging implementation, you can remove the comments if you have a warning related to loggers
    //implementation("ch.qos.logback:logback-classic:1.2.11")

    implementation("%s:%s:%s")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.isIncremental = true

    //JDA supports Java 8 and above
    sourceCompatibility = "1.8"
}