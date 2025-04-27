package dev.freya02.doxxy.common.dto

import dev.freya02.doxxy.common.DocumentedExampleLibrary
import dev.freya02.doxxy.common.QualifiedPartialIdentifier
import dev.freya02.doxxy.common.SimpleClassName
import kotlinx.serialization.Serializable

@Serializable
data class MissingTargetsDTO(
    val sourceTypeToSimpleClassNames: Map<DocumentedExampleLibrary, Set<SimpleClassName>>,
    val sourceTypeToPartialIdentifiers: Map<DocumentedExampleLibrary, Set<QualifiedPartialIdentifier>>
)