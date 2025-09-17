package dev.freya02.doxxy.docs.utils

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal object HttpUtils {
    val CLIENT: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(2, TimeUnit.MINUTES) //Jitpack builds have a blocking read
        .build()

    fun removeFragment(url: String): String = url.substringBefore('#')
}
