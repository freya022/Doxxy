package com.freya02.bot.utils

import org.intellij.lang.annotations.Language

class QueryUnion constructor() {
    private val queries: MutableList<String> = arrayListOf()
    private val params: MutableList<Any> = arrayListOf()

    val finalQuery: String
        get() = queries.joinToString(prefix = "(", separator = ") union all (", postfix = ")")
    val finalParameters
        get() = params

    constructor(@Language("PostgreSQL") query: String, vararg params: Any) : this() {
        addQuery(query, *params)
    }

    constructor(@Language("PostgreSQL") query: String, params: List<Any>) : this(query, *params.toTypedArray())

    fun addSuffix(suffix: String, vararg params: Any): QueryUnion {
        return QueryUnion("$finalQuery $suffix", this.params + params)
    }

    fun addQuery(@Language("PostgreSQL") query: String, vararg params: Any) {
        this.queries += query
        this.params.addAll(params)
    }

    fun isUnion() = queries.size >= 2

    operator fun component1() = finalQuery
    operator fun component2() = finalParameters
}
