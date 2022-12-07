package com.freya02.bot

import com.freya02.bot.TestUtils.measureTime
import com.freya02.bot.utils.HttpUtils.getDocument
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.jsoup.safety.Safelist
import java.nio.file.Files
import java.nio.file.StandardOpenOption

object DocsTest2 {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val document = getDocument("https://ci.dv8tion.net/job/JDA/javadoc/allclasses.html")
        val input = Main.BOT_FOLDER.resolve("docs_cache\\ci.dv8tion.net\\job\\JDA\\javadoc\\constant-values.html")
        val output = Main.BOT_FOLDER.resolve("docs_cache\\ci.dv8tion.net\\job\\JDA\\javadoc\\test.html")

        val html = Files.readString(input)
        val clean = Jsoup.clean(html, "https://ci.dv8tion.net/job/JDA/javadoc/allclasses.html", Safelist.relaxed())

        Files.writeString(output, clean, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

        val parser = Parser.htmlParser()
        measureTime("dirty doc", 1000, 1000) {
            val dirtyDoc = Jsoup.parse(html, "https://ci.dv8tion.net/job/JDA/javadoc/allclasses.html", parser)
        }

        measureTime("clean doc", 1000, 1000) {
            val cleanDoc = Jsoup.parse(clean, "https://ci.dv8tion.net/job/JDA/javadoc/allclasses.html", parser)
        }

        println()
    }
}