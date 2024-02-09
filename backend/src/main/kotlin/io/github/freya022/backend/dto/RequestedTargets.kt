package io.github.freya022.backend.dto

import io.github.freya022.backend.service.DocSourceType
import io.github.freya022.backend.service.QualifiedPartialIdentifier
import io.github.freya022.backend.service.SimpleClassName
import kotlinx.serialization.Serializable

@Serializable
data class RequestedTargets(
    val sourceTypeToSimpleClassNames: Map<DocSourceType, List<SimpleClassName>>,
    val sourceTypeToQualifiedPartialIdentifiers: Map<DocSourceType, List<QualifiedPartialIdentifier>>
)