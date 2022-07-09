package com.freya02.docs2

import com.freya02.docs.DocsSession
import com.freya02.docs.data.ClassDoc

fun main() {
//    val page =
//        PageCache.getPage("https://ci.dv8tion.net/job/JDA5/javadoc/net/dv8tion/jda/api/entities/Activity.Timestamps.html")
    val session = DocsSession()
    val doc = ClassDoc(
        session,
        "https://ci.dv8tion.net/job/JDA5/javadoc/net/dv8tion/jda/api/entities/Activity.Timestamps.html"
    )

    println()
}