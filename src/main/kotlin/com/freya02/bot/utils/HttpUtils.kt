package com.freya02.bot.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.concurrent.TimeUnit

object HttpUtils {
    @JvmField
    val CLIENT: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(2, TimeUnit.MINUTES) //Jitpack builds have a blocking read
        .build()

    @Synchronized
    fun parseDocument(downloadedBody: String, baseUri: String): Document {
        return Jsoup.parse(downloadedBody, baseUri)
    }

    @JvmStatic
    @Synchronized
    @Throws(IOException::class)
    fun getDocument(url: String): Document {
        return parseDocument(downloadBody(url), url)
    }

    @Throws(IOException::class)
    fun downloadBody(url: String): String {
        CLIENT.newCall(
            Request.Builder()
                .url(url)
                .build()
        ).execute().use { response ->
            val body = response.body ?: throw IllegalArgumentException("Got no body from url: $url")
            return body.string()
        }
    }

    fun doesStartByLocalhost(link: String): Boolean {
        return link.startsWith("http://localhost")
    }

    fun removeFragment(url: String): String = url.substringBefore('#')
}