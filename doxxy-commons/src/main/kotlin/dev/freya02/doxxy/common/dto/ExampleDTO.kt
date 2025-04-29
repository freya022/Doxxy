package dev.freya02.doxxy.common.dto

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
        val parts: List<ExampleContentPartDTO>
    ) {
        @Serializable
        data class ExampleContentPartDTO(
            val content: String,
            val label: String,
            val emoji: String?,
            val description: String?
        )
    }
}