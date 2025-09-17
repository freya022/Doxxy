package dev.freya02.doxxy.docs.utils

import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

internal object HttpUtils {
    val CLIENT: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(2, TimeUnit.MINUTES) //Jitpack builds have a blocking read
        .build()

    @Synchronized
    fun parseDocument(downloadedBody: String, baseUri: String): Document {
        return Jsoup.parse(downloadedBody, baseUri)
    }

    fun removeFragment(url: String): String = url.substringBefore('#')
}
