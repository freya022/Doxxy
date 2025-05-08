package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.HTMLElement.Companion.replaceScriptCharactersOrNull

fun main() {
    println("abcdefghijklmnopqrstuvwxyz0123456789+-=()".replaceScriptCharactersOrNull(HTMLElement.superscripts))
    println("abcdefghijklmnopqrstuvwxyz0123456789+-=()".replaceScriptCharactersOrNull(HTMLElement.subscripts))
    println("−w".replaceScriptCharactersOrNull(HTMLElement.superscripts))
    println("−w".replaceScriptCharactersOrNull(HTMLElement.subscripts))
}