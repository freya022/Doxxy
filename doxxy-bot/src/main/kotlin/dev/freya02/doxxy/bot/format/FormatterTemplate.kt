package dev.freya02.doxxy.bot.format

class FormatterTemplate private constructor(private val template: String, val extractRegex: Regex) {
    constructor(template: String, extractRegex: String) : this(template, extractRegex.toRegex())

    fun withSource(userSource: String) = template.replace("{}", userSource)
}