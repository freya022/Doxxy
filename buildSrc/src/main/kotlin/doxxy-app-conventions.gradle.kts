plugins {
    kotlin("jvm")
    id("com.google.cloud.tools.jib")
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.5.16")
    implementation("dev.reformator.stacktracedecoroutinator:stacktrace-decoroutinator-jvm:2.4.8")
}
