package dev.freya02.doxxy.bot.versioning.github

import kotlinx.serialization.Serializable

@Serializable
data class Release(val tagName: String)
