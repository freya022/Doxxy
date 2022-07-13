package com.freya02.docs

import com.freya02.bot.Main
import com.freya02.bot.utils.HttpUtils
import com.freya02.bot.utils.Utils.deleteRecursively
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.jsoup.nodes.Document
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object PageCache {
    private val cachePath = Main.PAGE_CACHE_FOLDER_PATH

    private val globalLock = ReentrantLock()
    private val pathMutexMap: MutableMap<Path, ReentrantLock> = ConcurrentHashMap()

    init {
        cachePath.createDirectories()
    }

    fun getPage(url: String): Document {
        val type = DocSourceType.fromUrl(url) ?: throw IllegalArgumentException("Unknown doc type for url '$url'")
        val baseFolder = getBaseFolder(type)

        val cachedFilePath = url.toHttpUrl().pathSegments.fold(baseFolder) { path, segment -> path.resolve(segment) }

        globalLock.lock() //Wait for clearing to stop
        globalLock.unlock()

        pathMutexMap
            .getOrPut(cachedFilePath) { ReentrantLock() }
            .withLock {
                val body = if (cachedFilePath.exists()) {
                    cachedFilePath.readText()
                } else {
                    HttpUtils.CLIENT.newCall(
                        Request.Builder()
                            .url(url)
                            .build()
                    ).execute().use { response ->
                        val body = response.body ?: throw IllegalArgumentException("Got no body from url: $url")
                        return@use body.string()
                    }.also { body ->
                        cachedFilePath.parent.createDirectories()
                        cachedFilePath.writeText(body)
                    }
                }

                return HttpUtils.parseDocument(body, url)
            }
    }

    fun clearCache(type: DocSourceType) {
        globalLock.withLock {
            getBaseFolder(type).deleteRecursively()
        }
    }

    private fun getBaseFolder(type: DocSourceType): Path {
        return cachePath.resolve(type.name)
    }
}