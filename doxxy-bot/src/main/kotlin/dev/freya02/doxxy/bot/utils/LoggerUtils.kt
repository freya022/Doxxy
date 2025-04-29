package dev.freya02.doxxy.bot.utils

import io.github.freya022.botcommands.api.core.Logging
import io.github.oshai.kotlinlogging.slf4j.internal.Slf4jLogger
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
        logger = (Logging.currentLogger() as Slf4jLogger<*>).underlyingLogger
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