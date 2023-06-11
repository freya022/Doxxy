package com.freya02.docs

import com.freya02.docs.data.ClassDoc

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