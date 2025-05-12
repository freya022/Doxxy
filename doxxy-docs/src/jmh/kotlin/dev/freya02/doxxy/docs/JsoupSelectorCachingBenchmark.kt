package dev.freya02.doxxy.docs

import org.jsoup.select.Evaluator
import org.jsoup.select.QueryParser
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
open class JsoupSelectorCachingBenchmark {

    @Param("#all-classes-table > div > div.summary-table.two-column-summary > div.col-first > a:nth-child(1)")
    lateinit var cssSelector: String

    // Even the biggest CSS selector used in the code only takes 2 µs to compile,
    // traversing the tree and applying the selector is magnitudes slower,
    // not worth caching those
    @Benchmark
    fun parse(): Evaluator {
        return QueryParser.parse(cssSelector)
    }
}