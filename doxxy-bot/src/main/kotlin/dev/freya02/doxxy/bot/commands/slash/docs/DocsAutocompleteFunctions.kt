package dev.freya02.doxxy.bot.commands.slash.docs

import dev.freya02.doxxy.bot.docs.index.DocIndex
import dev.freya02.doxxy.bot.docs.index.DocSearchResult

suspend fun classNameAutocomplete(index: DocIndex, query: String, limit: Int = 25) =
    index.getClasses(query, limit)

suspend fun methodOrFieldByClassAutocomplete(index: DocIndex, className: String, query: String, limit: Int = 25): List<DocSearchResult> =
    index.findMethodAndFieldSignaturesIn(className, query, limit)

suspend fun searchAutocomplete(index: DocIndex, query: String) =
    index.search(query)
