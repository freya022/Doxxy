package com.freya02.docs

import com.freya02.bot.docs.SourceRootMetadata
import com.freya02.bot.utils.Utils.measureTime
import kotlin.io.path.Path

fun main() {
    val sourceRootMetadata = measureTime("lol") {
         SourceRootMetadata(Path("C:\\Users\\freya02\\Programming\\IntelliJ-Workspace\\Forks\\JDA-Stuff\\JDA\\src\\main\\java"))
    }

    println(sourceRootMetadata)
}