package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.data.ClassDoc

fun main() {
//    val page =
//        PageCache.getPage("https://docs.jda.wiki/net/dv8tion/jda/api/entities/Activity.Timestamps.html")
    val session = DocsSession()
    val doc = ClassDoc(
        session,
        "https://docs.jda.wiki/net/dv8tion/jda/api/entities/Activity.Timestamps.html"
    )

    println()
}