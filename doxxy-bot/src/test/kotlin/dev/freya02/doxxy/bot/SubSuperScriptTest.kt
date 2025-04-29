package dev.freya02.doxxy.bot

import dev.freya02.doxxy.docs.HTMLElement.Companion.replaceScriptCharactersOrNull
import dev.freya02.doxxy.docs.HTMLElement.Companion.subscripts
import dev.freya02.doxxy.docs.HTMLElement.Companion.superscripts

fun main() {
    println("abcdefghijklmnopqrstuvwxyz0123456789+-=()".replaceScriptCharactersOrNull(superscripts))
    println("abcdefghijklmnopqrstuvwxyz0123456789+-=()".replaceScriptCharactersOrNull(subscripts))
    println("−w".replaceScriptCharactersOrNull(superscripts))
    println("−w".replaceScriptCharactersOrNull(subscripts))
}