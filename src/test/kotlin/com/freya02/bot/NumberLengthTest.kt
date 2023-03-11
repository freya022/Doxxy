package com.freya02.bot

import java.io.OutputStream
import java.io.PrintStream
import kotlin.math.floor
import kotlin.math.log10

// https://blogs.oracle.com/javamagazine/post/java-hotspot-hsdis-disassembler
// -XX:+UnlockDiagnosticVMOptions -XX:PrintAssemblyOptions=intel -XX:CompileCommand="print,com/freya02/bot/NumberLengthTest.mathInteger" -XX:CompileCommand="dontinline,com/freya02/bot/NumberLengthTest.mathInteger" -XX:CompileCommand="print,com/freya02/bot/NumberLengthTest.mathFloor" -XX:CompileCommand="dontinline,com/freya02/bot/NumberLengthTest.mathFloor" -XX:CompileCommand="print,com/freya02/bot/NumberLengthTest.mathString" -XX:CompileCommand="dontinline,com/freya02/bot/NumberLengthTest.mathString"
object NumberLengthTest {
    @JvmStatic
    fun main(args: Array<out String>) {
//    val length = 1000000
        val length = 9999999
        println(mathFloor(length))
        println(mathInteger(length))
        println(mathString(length))

        val sb = StringBuilder(1024 * 1024 * 64)

        println(sb.capacity())

        TestUtils.measureTime("Math integer part", 10000000, 10000000) {
            sb.append(mathInteger(length))
        }

        sb.clear()

        TestUtils.measureTime("Math floor", 10000000, 10000000) {
            sb.append(mathFloor(length))
        }

        sb.clear()

        TestUtils.measureTime("String", 10000000, 10000000) {
            sb.append(mathString(length))
        }

        System.setOut(PrintStream(OutputStream.nullOutputStream()))
        println(sb.toString())

        println(sb.capacity())
    }

    @JvmStatic
    private fun mathString(length: Int) = length.toString().length

    @JvmStatic
    private fun mathFloor(length: Int) = 1 + floor(log10(length.toDouble()))

    @JvmStatic
    private fun mathInteger(length: Int) = 1 + log10(length.toDouble()).toInt()
}