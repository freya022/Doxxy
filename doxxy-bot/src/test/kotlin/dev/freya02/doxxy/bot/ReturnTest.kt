package dev.freya02.doxxy.bot

import io.github.freya022.botcommands.api.commands.application.context.message.GuildMessageEvent
import javassist.ClassPool
import javassist.CtClass
import javassist.expr.ExprEditor
import javassist.expr.MethodCall
import java.lang.invoke.MethodHandles

object ReturnTest {
    fun method3(): Double {
        println("It was me, method 3 !")

        return 2.71
    }

    fun method2(): Double {
        println("m2")

        throw AssertionError("Nope")
    }

    fun method1(): Int {
        println("m1")

        method2()

        println("m1 end")

        return 1
    }

    @JvmStatic
    fun reply(string: String) {
        println("Replied: $string")
    }

    @JvmStatic
    fun replyAndReturn(string: String): Nothing {
        throw AssertionError("Nothing")
    }

    fun testEarlyReturn() {
        replyAndReturn("This is a reply message")

        throw AssertionError("Unreachable")
    }

    fun Any?.doReturn(): Nothing {
        throw AssertionError("Unreachable")
    }

    fun testForceReturn() {
        dummy().doReturn()

        throw AssertionError("Unreachable")
    }

    private fun dummy() {}

    fun useCase(event: GuildMessageEvent, inputStr: String) {
        val number = inputStr.toIntOrNull() ?: let {
            event.reply("Invalid number").queue()
            return
        }

        println(number)
    }

    fun useCase2(event: GuildMessageEvent, inputStr: String) {
        val number = inputStr.toIntOrNull() ?: event.reply("Invalid number").queue().doReturn()

        println(number)
    }
}

// https://stackoverflow.com/questions/28149625/replacing-a-java-method-invocation-from-a-field-with-a-method-call
// https://github.com/jboss-javassist/javassist/wiki/Tutorial-2#42-altering-a-method-body
fun main() {
    val pool = ClassPool.getDefault()
    val ctClass = pool["dev.freya02.doxxy.bot.ReturnTest"]
//    overrideReturnTest(ctClass)

//    earlyReturnTest(ctClass)

    forceReturnTest(ctClass)

    //Apparently this does rewrite the class
    ctClass.toClass(MethodHandles.lookup())

//    ReturnTest.testEarlyReturn()

    ReturnTest.testForceReturn()
}

private fun overrideReturnTest(ctClass: CtClass) {
    val method = ctClass.getDeclaredMethod("method1")

    method.instrument(object : ExprEditor() {
        override fun edit(m: MethodCall) {
            if (m.enclosingClass == ctClass && m.methodName == "method2") {
                m.replace("{ \$_ = 2.71; return 5; }")
            }
        }
    })
}

private fun earlyReturnTest(ctClass: CtClass) {
    val testMethod = ctClass.getDeclaredMethod("testEarlyReturn")
    testMethod.instrument(object : ExprEditor() {
        private val regex = Regex("(.*)AndReturn")

        override fun edit(m: MethodCall) {
            if (m.enclosingClass == ctClass) {
                val matchResult = regex.matchEntire(m.methodName) ?: return
                val underlyingMethodName = matchResult.groupValues[1]

                //Change this string for java methods
                m.replace("{ $underlyingMethodName($$); \$_ = kotlin.Unit.INSTANCE; return; }")
            }
        }
    })
}

private fun forceReturnTest(ctClass: CtClass) {
    val testMethod = ctClass.getDeclaredMethod("testForceReturn")
    testMethod.instrument(object : ExprEditor() {
        override fun edit(m: MethodCall) {
            if (m.enclosingClass == ctClass && m.methodName == "return") {
                //Change this string for java methods
                m.replace("{ \$_ = kotlin.Unit.INSTANCE; return; }")
            }
        }
    })
}