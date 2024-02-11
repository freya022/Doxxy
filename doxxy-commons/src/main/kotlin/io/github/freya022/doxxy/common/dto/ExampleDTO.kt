package io.github.freya022.doxxy.common.dto

import kotlinx.serialization.Serializable

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