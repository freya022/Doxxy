package dev.freya02.doxxy.backend.transformer

import dev.freya02.doxxy.backend.entity.Example
import dev.freya02.doxxy.backend.entity.ExampleContent
import dev.freya02.doxxy.backend.entity.ExampleContentPart
import dev.freya02.doxxy.common.dto.ExampleDTO

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