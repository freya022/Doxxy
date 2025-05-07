package dev.freya02.doxxy.docs.utils

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.util.concurrent.TimeUnit

internal object HttpUtils {
    val CLIENT: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(2, TimeUnit.MINUTES) //Jitpack builds have a blocking read
        .build()

    @Synchronized
    fun parseDocument(downloadedBody: String, baseUri: String): Document {
        return Jsoup.parse(downloadedBody, baseUri)
    }

    @Synchronized
    @Throws(IOException::class)
    fun getDocument(url: String): Document {
        return parseDocument(downloadBody(url), url)
    }

    fun <R> doRequest(request: Request, handleNonSuccess: Boolean = true, block: (Response, ResponseBody) -> R): R {
        CLIENT.newCall(request).execute().use { response ->
            if (handleNonSuccess) {
                if (!response.isSuccessful) throw IOException("Got an unsuccessful response from ${response.request.url}, code: ${response.code}")
            }

            val body: ResponseBody = response.body
                ?: throw IOException("Got no ResponseBody for ${response.request.url}")

            return block(response, body)
        }
    }

    fun <R> doRequest(url: String, handleNonSuccess: Boolean = true, block: (Response, ResponseBody) -> R): R {
        return doRequest(Request.Builder().url(url).build(), handleNonSuccess, block)
    }

    @Throws(IOException::class)
    fun downloadBody(url: String): String = doRequest(url) { _, body ->
        body.string()
    }

    fun doesStartByLocalhost(link: String): Boolean {
        return link.startsWith("http://localhost")
    }

    fun removeFragment(url: String): String = url.substringBefore('#')
}