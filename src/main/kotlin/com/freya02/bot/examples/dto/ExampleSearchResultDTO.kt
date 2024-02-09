package com.freya02.bot.examples.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExampleSearchResultDTO(
    val title: String,
    val library: String
)