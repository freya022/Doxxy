package com.freya02.bot.docs.index

import com.freya02.bot.utils.CryptoUtils
import java.nio.file.Path

class IndexPaths(private val sourceCacheFolder: Path) {
    fun getClassEmbedPath(className: String): Path {
        return sourceCacheFolder.resolve(className).resolve("ClassEmbed.json")
    }

    fun getMethodEmbedPath(className: String, methodId: String): Path {
        return sourceCacheFolder.resolve(className).resolve(getMethodFileName(methodId))
    }

    fun getFieldEmbedPath(className: String, fieldName: String): Path {
        return sourceCacheFolder.resolve(className).resolve(getFieldFileName(fieldName))
    }

    fun getMethodFileName(signature: String): String {
        return CryptoUtils.hash(signature) + ".json"
    }

    fun getFieldFileName(fieldName: String): String {
        return CryptoUtils.hash(fieldName) + ".json"
    }
}