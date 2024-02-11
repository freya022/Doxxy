package io.github.freya022.doxxy.common.dto

import io.github.freya022.doxxy.common.DocumentedExampleLibrary
import io.github.freya022.doxxy.common.QualifiedPartialIdentifier
import io.github.freya022.doxxy.common.SimpleClassName
import kotlinx.serialization.Serializable

@Serializable
data class RequestedTargetsDTO(
    val sourceTypeToSimpleClassNames: Map<DocumentedExampleLibrary, Set<SimpleClassName>>,
    val sourceTypeToQualifiedPartialIdentifiers: Map<DocumentedExampleLibrary, Set<QualifiedPartialIdentifier>>
)