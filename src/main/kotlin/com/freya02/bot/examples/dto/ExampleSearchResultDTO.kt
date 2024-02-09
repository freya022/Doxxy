package com.freya02.bot.examples.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExampleSearchResultDTO(
    val title: String,
    val library: String //TODO use enumeration once there's a common module
)