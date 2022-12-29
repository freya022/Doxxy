package com.freya02.bot

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
}

// https://stackoverflow.com/questions/28149625/replacing-a-java-method-invocation-from-a-field-with-a-method-call
// https://github.com/jboss-javassist/javassist/wiki/Tutorial-2#42-altering-a-method-body
fun main() {
    val pool = ClassPool.getDefault()
    val ctClass = pool["com.freya02.bot.ReturnTest"]
//    overrideReturnTest(ctClass)

    earlyReturnTest(ctClass)

    //Apparently this does rewrite the class
    ctClass.toClass(MethodHandles.lookup())

    ReturnTest.testEarlyReturn()
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