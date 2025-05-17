package dev.freya02.doxxy.docs

import java.nio.file.Path
import kotlin.io.path.toPath

fun getResourcePath(resource: String): Path {
    return IntegrationTests::class.java.getResource(resource)!!.toURI().toPath()
}