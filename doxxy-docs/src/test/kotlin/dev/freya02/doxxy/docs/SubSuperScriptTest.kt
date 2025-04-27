package dev.freya02.doxxy.docs

import dev.freya02.doxxy.docs.HTMLElement.Companion.replaceScriptCharactersOrNull

fun main() {
    println("abcdefghijklmnopqrstuvwxyz0123456789+-=()".replaceScriptCharactersOrNull(HTMLElement.Companion.superscripts))
    println("abcdefghijklmnopqrstuvwxyz0123456789+-=()".replaceScriptCharactersOrNull(HTMLElement.Companion.subscripts))
    println("−w".replaceScriptCharactersOrNull(HTMLElement.Companion.superscripts))
    println("−w".replaceScriptCharactersOrNull(HTMLElement.Companion.subscripts))
}