package com.freya02.bot.commands.slash.docs

import com.freya02.bot.docs.index.DocIndex
import com.freya02.bot.docs.index.DocSearchResult

suspend inline fun classNameAutocomplete(index: DocIndex, query: String, limit: Int = 25) =
    index.getClasses(query, limit)

suspend inline fun classNameWithMethodsAutocomplete(index: DocIndex, query: String) =
    index.getClassesWithMethods(query)

suspend inline fun classNameWithFieldsAutocomplete(index: DocIndex, query: String) =
    index.getClassesWithFields(query)

suspend inline fun methodNameByClassAutocomplete(index: DocIndex, className: String, query: String, limit: Int = 25) =
    index.findMethodSignaturesIn(className, query, limit)

suspend inline fun fieldNameByClassAutocomplete(index: DocIndex, className: String, query: String, limit: Int = 25) =
    index.findFieldSignaturesIn(className, query, limit)

suspend inline fun methodOrFieldByClassAutocomplete(index: DocIndex, className: String, query: String, limit: Int = 25): List<DocSearchResult> =
    index.findMethodAndFieldSignaturesIn(className, query, limit)

suspend inline fun anyMethodNameAutocomplete(index: DocIndex, query: String, limit: Int = 25) =
    index.findAnyMethodSignatures(query, limit)

suspend inline fun anyFieldNameAutocomplete(index: DocIndex, query: String, limit: Int = 25) =
    index.findAnyFieldSignatures(query, limit)