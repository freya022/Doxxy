package com.freya02.bot

import com.freya02.bot.TestUtils.measureTime
import com.freya02.bot.utils.CryptoUtils.toHexString
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object CryptoTest {
    private const val STR = "setTimeout(long, TimeUnit, Consumer)"
    private val md5: MessageDigest = MessageDigest.getInstance("MD5")
    private val sha1: MessageDigest = MessageDigest.getInstance("SHA-1")
    private val sha256: MessageDigest = MessageDigest.getInstance("SHA-256")
    private val sha512: MessageDigest = MessageDigest.getInstance("SHA-512")
    private val sha3_256: MessageDigest = MessageDigest.getInstance("SHA3-256")

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        formatTest()

        measureTime("MD5", 100000, 10000) {
            val digest = md5.digest(STR.toByteArray(StandardCharsets.UTF_8))
            toHexString(digest)
        }

        measureTime("SHA1", 100000, 10000) {
            val digest = sha1.digest(STR.toByteArray(StandardCharsets.UTF_8))
            toHexString(digest)
        }

        measureTime("SHA256", 100000, 10000) {
            val digest = sha256.digest(STR.toByteArray(StandardCharsets.UTF_8))
            toHexString(digest)
        }

        measureTime("SHA512", 100000, 10000) {
            val digest = sha512.digest(STR.toByteArray(StandardCharsets.UTF_8))
            toHexString(digest)
        }

        measureTime("SHA3-256", 100000, 10000) {
            val digest = sha3_256.digest(STR.toByteArray(StandardCharsets.UTF_8))
            toHexString(digest)
        }
    }

    private fun formatTest() {
        val digest = sha512.digest(STR.toByteArray(StandardCharsets.UTF_8))
        measureTime("String.format", 100000, 100000) { String.format("%032X", BigInteger(1, digest)) }
        measureTime("String builder", 100000, 100000) { toHexString(digest) }
    }
}