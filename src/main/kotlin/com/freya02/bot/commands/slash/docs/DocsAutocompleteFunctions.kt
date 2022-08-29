package com.freya02.bot.commands.slash.docs

import com.freya02.bot.docs.index.DocIndex
import com.freya02.bot.docs.index.DocSearchResult

fun classNameAutocomplete(index: DocIndex, query: String, limit: Int = 25) =
    index.getClasses(query, limit)

fun classNameWithMethodsAutocomplete(index: DocIndex, query: String) =
    index.getClassesWithMethods(query)

fun classNameWithFieldsAutocomplete(index: DocIndex, query: String) =
    index.getClassesWithFields(query)

fun methodNameByClassAutocomplete(index: DocIndex, className: String, query: String, limit: Int = 25) =
    index.findMethodSignaturesIn(className, query, limit)

fun fieldNameByClassAutocomplete(index: DocIndex, className: String, query: String, limit: Int = 25) =
    index.findFieldSignaturesIn(className, query, limit)

fun methodOrFieldByClassAutocomplete(index: DocIndex, className: String, query: String, limit: Int = 25): List<DocSearchResult> =
    index.findMethodAndFieldSignaturesIn(className, query, limit)

fun anyMethodNameAutocomplete(index: DocIndex, query: String, limit: Int = 25) =
    index.findAnyMethodSignatures(query, limit)

fun anyFieldNameAutocomplete(index: DocIndex, query: String, limit: Int = 25) =
    index.findAnyFieldSignatures(query, limit)