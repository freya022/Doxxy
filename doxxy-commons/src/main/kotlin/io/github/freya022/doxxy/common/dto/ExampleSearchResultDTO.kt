package io.github.freya022.doxxy.common.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExampleSearchResultDTO(
    val title: String,
    val library: String, //TODO use enumeration
    val languages: List<String>
)