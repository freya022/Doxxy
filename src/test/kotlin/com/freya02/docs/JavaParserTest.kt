package com.freya02.docs

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.utils.SourceRoot
import kotlin.io.path.Path

fun main() {
    val sourceRoot = SourceRoot(Path("C:\\Users\\freya02\\Programming\\IntelliJ-Workspace\\Forks\\JDA-Stuff\\JDA\\src\\main\\java"))

    val compilationUnit = sourceRoot.parse("net.dv8tion.jda.api.utils", "AllowedMentions.java")

    compilationUnit.accept(object : VoidVisitorAdapter<Void>() {
        override fun visit(n: MethodDeclaration, arg: Void?) {


            super.visit(n, arg)
        }
    }, null)
}