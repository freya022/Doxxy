package io.github.freya022.backend.dto

import io.github.freya022.backend.entity.Example
import kotlinx.serialization.Serializable

@Serializable
data class ExampleDTO(
    val title: String,
    val contents: List<ExampleContentDTO>
) {
    @Serializable
    data class ExampleContentDTO(
        val language: String,
        val content: String
    )

    constructor(example: Example) : this(
        example.title,
        example.contents.map { ExampleContentDTO(it.language, it.content) }
    )
}