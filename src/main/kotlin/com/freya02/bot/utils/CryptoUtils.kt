package com.freya02.bot.utils

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*

object CryptoUtils {
    private val SHA3_256: MessageDigest = MessageDigest.getInstance("SHA3-256")

    fun hash(content: ByteArray): String {
        return toHexString(SHA3_256.digest(content))
    }

    @JvmStatic
    fun toHexString(bytes: ByteArray): String {
        val builder = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val hex = 0xFF and b.toInt()
            if (hex < 0x10) {
                builder.append("0")
            }
            builder.append(Integer.toHexString(hex).uppercase(Locale.getDefault()))
        }

        return builder.toString()
    }

    fun hash(content: String): String {
        return hash(content.toByteArray(StandardCharsets.UTF_8))
    }
}