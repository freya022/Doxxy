package com.freya02.bot

import javax.script.ScriptEngineManager
import javax.script.ScriptException

object ScriptingTest {
    @JvmStatic
    suspend fun myMethod() {
        println("Hello world!")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val engine = ScriptEngineManager().getEngineByExtension("kts")!!

        engine.put("value", this)

        println(engine.eval("val x = 2 + 2"))
        println(engine.eval("x"))
        println(engine.eval("run { x }"))
        println(engine.eval("this"))
        println(engine.eval("value"))
        try {
            println(engine.eval("TODO()"))
        } catch (e: ScriptException) {
            println("Expected exception received")
        }
        try {
            println(engine.eval("notAVariable"))
        } catch (e: Exception) {
            println("Expected exception received")
        }
        engine.eval("kotlinx.coroutines.runBlocking { ${this::class.java.name}.myMethod() }")
    }
}