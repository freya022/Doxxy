package dev.freya02.doxxy.docs

import dev.freya02.doxxy.common.Directories
import dev.freya02.doxxy.docs.utils.HttpUtils
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.*

class PageCache private constructor(name: String) {
    private val globalLock = ReentrantLock()
    private val pathMutexMap: MutableMap<Path, ReentrantLock> = ConcurrentHashMap()

    private val baseFolder = Directories.pageCache.resolve(name)

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
                        check(response.isSuccessful) { "Failed to get '$url': ${response.code} ${response.message}" }
                        val body = response.body ?: throw IllegalArgumentException("Got no body from url: $url")
                        return@use body.string()
                    }.also { body ->
                        cachedFilePath.parent.createDirectories()
                        cachedFilePath.writeText(body)
                    }
                }
            }

            Jsoup.parse(body, url)
        }
    }

    @OptIn(ExperimentalPathApi::class)
    fun clearCache() {
        globalLock.withLock {
            baseFolder.deleteRecursively()
        }
    }

    companion object {
        private val map: MutableMap<String, PageCache> = hashMapOf()

        operator fun get(name: String): PageCache {
            return map.getOrPut(name) { PageCache(name) }
        }

        operator fun get(source: JavadocSource): PageCache {
            return map.getOrPut(source.name) { PageCache(source.name) }
        }
    }
}
