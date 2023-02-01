package com.freya02.bot.commands.slash.docs

import com.freya02.bot.docs.index.DocIndex
import com.freya02.bot.docs.index.DocSearchResult
import com.freya02.bot.docs.index.DocTypes

suspend fun classNameAutocomplete(index: DocIndex, query: String, limit: Int = 25) =
    index.getClasses(query, limit)

suspend fun methodOrFieldByClassAutocomplete(index: DocIndex, className: String, query: String, limit: Int = 25): List<DocSearchResult> =
    index.findMethodAndFieldSignaturesIn(className, query, limit)

suspend fun searchAutocomplete(index: DocIndex, query: String, limit: Int = 25, docTypes: DocTypes) =
    index.findAnySignatures(query, limit, docTypes)
