package io.github.freya022.backend.transformer

import io.github.freya022.backend.entity.Example
import io.github.freya022.backend.entity.ExampleContent
import io.github.freya022.backend.entity.ExampleContentPart
import io.github.freya022.doxxy.common.dto.ExampleDTO

fun Example.toDTO() = ExampleDTO(
    title,
    library,
    contents.map { it.toDTO() }
)

fun ExampleContent.toDTO() = ExampleDTO.ExampleContentDTO(
    language,
    parts.map { it.toDTO() }
)

fun ExampleContentPart.toDTO() = ExampleDTO.ExampleContentDTO.ExampleContentPartDTO(content, label, emoji, description)