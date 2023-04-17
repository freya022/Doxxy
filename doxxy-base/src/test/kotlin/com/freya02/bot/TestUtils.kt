package com.freya02.bot

import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureNanoTime

object TestUtils {
    /**
     * Mesures time to run a method
     *
     * @param desc       Description of the code
     * @param warmup     Number of warmup iterations
     * @param iterations Number of real iterations
     * @param code       The code to run
     * @return The average time in ms
     */
    @JvmStatic
    fun measureTime(desc: String, warmup: Int, iterations: Int, code: Runnable): Double {
        for (i in 0 until warmup) {
            code.run()
        }

        var worst = Long.MIN_VALUE
        var best = Long.MAX_VALUE
        var total: Long = 0
        for (i in 0 until iterations) {
            val elapsed = measureNanoTime {
                code.run()
            }

            worst = max(worst, elapsed)
            best = min(best, elapsed)
            total += elapsed
        }

        val average = total / 1000000.0 / iterations
        System.out.printf(
            "$desc : Iterations : %s, Best : %.7f ms, Worst : %.7f ms, Average : %.7f ms, Total : %.7f ms%n",
            iterations,
            best / 1000000.0,
            worst / 1000000.0,
            average,
            total / 1000000.0
        )
        return average
    }
}