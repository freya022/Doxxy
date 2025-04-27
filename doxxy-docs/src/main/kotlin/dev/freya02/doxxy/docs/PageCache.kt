package dev.freya02.doxxy.docs

import dev.freya02.doxxy.bot.config.Data
import dev.freya02.doxxy.docs.utils.HttpUtils
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.jsoup.nodes.Document
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.*

class PageCache(val type: DocSourceType) {
    private val globalLock = ReentrantLock()
    private val pathMutexMap: MutableMap<Path, ReentrantLock> = ConcurrentHashMap()

    private val baseFolder = Data.getCacheFolder(type)

    private fun <R> withLockedPath(url: String, block: (Path) -> R): R {
        val cachedFilePath = url.toHttpUrl().let { httpUrl ->
            httpUrl.pathSegments.fold(baseFolder.resolve(httpUrl.host)) { path, segment -> path.resolve(segment) }
        }

        globalLock.lock() //Wait for clearing to stop
        globalLock.unlock()

        return pathMutexMap
            .getOrPut(cachedFilePath) { ReentrantLock() }
            .withLock { block(cachedFilePath) }
    }

    fun getRawOrNull(url: String): ByteArray? {
        return withLockedPath(url) { cachedFilePath ->
            when {
                cachedFilePath.exists() -> cachedFilePath.readBytes()
                else -> {
                    HttpUtils.CLIENT.newCall(
                        Request.Builder()
                            .url(url)
                            .build()
                    ).execute().use { response ->
                        if (!response.isSuccessful) return@use null
                        val body = response.body ?: return@use null
                        return@use body.bytes()
                    }.also { bytes ->
                        if (bytes == null) return@also

                        cachedFilePath.parent.createDirectories()
                        cachedFilePath.writeBytes(bytes)
                    }
                }
            }
        }
    }

    fun getPage(url: String): Document {
        return withLockedPath(url) { cachedFilePath ->
            val body = when {
                cachedFilePath.exists() -> cachedFilePath.readText()
                else -> {
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
            }

            HttpUtils.parseDocument(body, url)
        }
    }

    @OptIn(ExperimentalPathApi::class)
    fun clearCache() {
        globalLock.withLock {
            baseFolder.deleteRecursively()
        }
    }

    companion object {
        private val map: MutableMap<DocSourceType, PageCache> = EnumMap(DocSourceType::class.java)

        operator fun get(type: DocSourceType): PageCache {
            return map.getOrPut(type) { PageCache(type) }
        }
    }
}