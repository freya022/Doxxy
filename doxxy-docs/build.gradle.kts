plugins {
    id("doxxy-conventions")
}

dependencies {
    implementation(projects.doxxy.doxxyCommons)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.bundles.slf4j)

    implementation(libs.okhttp)

    implementation(libs.jsoup)

    implementation(libs.javaparser.core)

    implementation(libs.jda)

    implementation(libs.remark.java)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.bytebuddy)

    testImplementation(libs.bundles.slf4j)
    testImplementation(libs.logback.classic)
}
