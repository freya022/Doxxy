package com.freya02.bot

import com.freya02.docs.HTMLElement.Companion.replaceScriptCharactersOrNull
import com.freya02.docs.HTMLElement.Companion.subscripts
import com.freya02.docs.HTMLElement.Companion.superscripts

fun main() {
    println("abcdefghijklmnopqrstuvwxyz0123456789+-=()".replaceScriptCharactersOrNull(superscripts))
    println("abcdefghijklmnopqrstuvwxyz0123456789+-=()".replaceScriptCharactersOrNull(subscripts))
    println("−w".replaceScriptCharactersOrNull(superscripts))
    println("−w".replaceScriptCharactersOrNull(subscripts))
}