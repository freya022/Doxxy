package com.freya02.bot.examples.dto

import kotlinx.serialization.Serializable

//TODO add to common module
@Serializable
data class ExampleDTO(
    val title: String,
    val library: String,
    val contents: List<ExampleContentDTO>
) {
    @Serializable
    data class ExampleContentDTO(
        val language: String,
        val content: String
    )
}