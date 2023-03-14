package com.freya02.docs

import com.freya02.bot.docs.metadata.ImplementationMetadata
import mu.KotlinLogging
import kotlin.io.path.Path

object ImplementationResolverTest {
    private val logger = KotlinLogging.logger { }

    @JvmStatic
    fun main(args: Array<String>) {
        val metadata2 = ImplementationMetadata.fromClasspath(
            listOf(
                Path(System.getProperty("user.home"), ".m2\\repository\\net\\dv8tion\\JDA\\5.0.0-beta.5_DEV\\JDA-5.0.0-beta.5_DEV.jar")
            )
        )

        println()
    }
}