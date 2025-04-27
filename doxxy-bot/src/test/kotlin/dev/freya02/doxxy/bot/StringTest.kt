package dev.freya02.doxxy.bot

import net.dv8tion.jda.api.interactions.commands.build.OptionData
import kotlin.math.max

object StringTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val a = "A".repeat(50)
        val b = "ABCD".repeat(256)
        val str = getChoiceName(a, b)

        println("str = $str")
        println("str.length() = " + str.length)

        println("str = " + getChoiceName("abc", "def"))
        println("str = " + getChoiceName("abc", "def".repeat(100)))
        println("str = " + getChoiceName("abc".repeat(100), "def"))
        println("str = " + getChoiceName("abc".repeat(100), "def".repeat(100)))
    }

    private fun getChoiceName(name: String, description: String): String {
        var a = name
        var b = description
        if (a.length + b.length + 5 > 100) {
            val min = max(0, OptionData.MAX_CHOICE_NAME_LENGTH - a.length - 5)
            b = when (min) {
                0 -> ""
                else -> b.substring(0, min)
            }
        }

        if (a.length > 100) {
            a = a.substring(0, 100)
        }

        val spaces = max(0, OptionData.MAX_CHOICE_NAME_LENGTH - a.length - b.length)
        return a + " ".repeat(spaces) + b
    }
}