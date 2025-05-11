package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.JavadocElement.Companion.replaceScriptCharactersOrNull

fun main() {
    println("abcdefghijklmnopqrstuvwxyz0123456789+-=()".replaceScriptCharactersOrNull(JavadocElement.superscripts))
    println("abcdefghijklmnopqrstuvwxyz0123456789+-=()".replaceScriptCharactersOrNull(JavadocElement.subscripts))
    println("−w".replaceScriptCharactersOrNull(JavadocElement.superscripts))
    println("−w".replaceScriptCharactersOrNull(JavadocElement.subscripts))
}