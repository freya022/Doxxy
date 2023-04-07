package com.freya02.bot.utils

import mu.KotlinLogging
import org.slf4j.profiler.Profiler
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <R> createProfiler(name: String, block: Profiler.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return Profiler(name).apply {
        logger = KotlinLogging.logger { }
    }.let { profiler ->
        profiler.block().also {
            profiler.stop()
            if (profiler.logger.isTraceEnabled) profiler.log()
        }
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <R> Profiler.nextStep(name: String, block: Profiler.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    start(name)
    return block()
}

@OptIn(ExperimentalContracts::class)
inline fun <R> Profiler.nestedProfiler(name: String, block: Profiler.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return startNested(name).block()
}