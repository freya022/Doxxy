package io.github.freya022.backend.service

import io.github.freya022.backend.entity.Example
import io.github.freya022.backend.entity.ExampleContent
import io.github.freya022.backend.entity.ExampleContentPart
import io.github.freya022.backend.entity.ExampleTarget
import io.github.freya022.backend.repository.ExampleRepository
import io.github.freya022.backend.transformer.toDTO
import io.github.freya022.doxxy.common.ExampleLibrary
import io.github.freya022.doxxy.common.QualifiedPartialIdentifier
import io.github.freya022.doxxy.common.SimpleClassName
import io.github.freya022.doxxy.common.dto.ExampleDTO
import io.github.freya022.doxxy.common.dto.ExampleSearchResultDTO
import io.github.freya022.doxxy.common.dto.MissingTargetsDTO
import io.github.freya022.doxxy.common.dto.RequestedTargetsDTO
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.client.bodyWithType

private val logger = KotlinLogging.logger { }

@Service
class ExamplesService(
    private val botClient: RestClient,
    private val exampleRepository: ExampleRepository,
    private val json: Json
) {
    private val client = RestClient.create()

    @Serializable
    private data class IndexedExample(
        val name: String,
        val languages: List<String>,
        val library: String,
        val title: String,
        val parts: List<Part> = emptyList(),
        val targets: List<String> = emptyList()
    ) {
        @Serializable
        data class Part(
            val fileName: String,
            val label: String,
            val emoji: String?,
            val description: String?
        )

        //TODO 1:1 mapping with the index json
        val exampleLibrary = when (library) {
            "JDA" -> ExampleLibrary.JDA
            "JDK" -> ExampleLibrary.JDK
            else -> throw IllegalArgumentException("Unknown library: $library")
        }
    }

    @Transactional
    fun updateExamples(ownerName: String, repoName: String, branchName: String) {
        logger.info { "Updating examples from $ownerName/$repoName on $branchName" }

        val repoClient = RestClient.builder()
            .baseUrl("https://raw.githubusercontent.com/{ownerName}/{repoName}/{branchName}/")
            .defaultUriVariables(
                mapOf(
                    "ownerName" to ownerName,
                    "repoName" to repoName,
                    "branchName" to branchName,
                )
            )
            .build()

        exampleRepository.removeAll()

        val examples: List<IndexedExample> = repoClient.get()
            .uri("/index.json")
            .retrieve()
            .body<String>()!!
            .let(json::decodeFromString)

        // Gets actual targets from the bot
        val requestedTargets = examples
            .filter { it.exampleLibrary.documentedLibrary != null }
            .groupBy { it.exampleLibrary.documentedLibrary!! }
            .let { examplesBySourceType ->
                RequestedTargetsDTO(
                    examplesBySourceType.mapValues { (_, examples) ->
                        examples.flatMap { it.targets }.filterNot { '#' in it }.mapTo(hashSetOf(), ::SimpleClassName)
                    },
                    examplesBySourceType.mapValues { (_, examples) ->
                        examples.flatMap { it.targets }.filter { '#' in it }.mapTo(hashSetOf(), ::QualifiedPartialIdentifier)
                    }
                )
            }
        val missingTargets: MissingTargetsDTO = botClient.post()
            .uri("/examples/targets/check")
            .bodyWithType(requestedTargets)
            .retrieve()
            .body<String>()!!
            .let(json::decodeFromString)

        missingTargets.sourceTypeToSimpleClassNames.forEach { (documentedExampleLibrary, simpleClassNames) ->
            if (simpleClassNames.isNotEmpty()) {
                logger.error { "Missing classes in $documentedExampleLibrary:\n${simpleClassNames.joinToString()}" }
            }
        }
        missingTargets.sourceTypeToPartialIdentifiers.forEach { (documentedExampleLibrary, partialIdentifiers) ->
            if (partialIdentifiers.isNotEmpty()) {
                logger.error { "Missing (possibly partial) identifiers in $documentedExampleLibrary:\n${partialIdentifiers.joinToString()}" }
            }
        }

        exampleRepository.saveAllAndFlush(examples.map { dto ->
            val contentEntities = dto.languages.map { language ->
                ExampleContent(language, getExampleContentParts(repoClient, dto, language))
            }
            val resolvableTargetEntities = dto.targets.filter {
                if ('#' !in it) {
                    // Filter out if class is missing
                    SimpleClassName(it) !in missingTargets.sourceTypeToSimpleClassNames[dto.exampleLibrary.documentedLibrary!!]!!
                } else {
                    // Filter out if partial identifier is missing
                    QualifiedPartialIdentifier(it) !in missingTargets.sourceTypeToPartialIdentifiers[dto.exampleLibrary.documentedLibrary!!]!!
                }
            }.map { ExampleTarget(it) }

            Example(dto.title, dto.library, contentEntities, resolvableTargetEntities).also { example ->
                contentEntities.forEach { it.example = example }
            }
        })
    }

    private fun getExampleContentParts(repoClient: RestClient, dto: IndexedExample, language: String): List<ExampleContentPart> {
        return if (dto.parts.isEmpty()) {
            listOf(
                ExampleContentPart(
                    dto.title,
                    emoji = null,
                    description = null,
                    repoClient.getExampleContent(dto.library, dto.name, language)
                )
            )
        } else {
            dto.parts.map {
                ExampleContentPart(
                    it.label,
                    it.emoji,
                    it.description,
                    repoClient.getExampleContentPart(dto.library, dto.name, language, it.fileName)
                )
            }
        }
    }

    private fun RestClient.getExampleContent(library: String, name: String, language: String): String = get()
        .uri("examples/{library}/{name}/{language}.md", mapOf(
            "library" to library,
            "name" to name,
            "language" to language,
        ))
        .retrieve()
        .body()!!

    private fun RestClient.getExampleContentPart(library: String, name: String, language: String, partName: String): String = get()
        .uri("examples/{library}/{name}/{language}/{partName}.md", mapOf(
            "library" to library,
            "name" to name,
            "language" to language,
            "partName" to partName,
        ))
        .retrieve()
        .body()!!

    fun findByTarget(target: String): List<ExampleSearchResultDTO> {
        return exampleRepository.findByTarget(target).map { it.toExampleSearchResultDTO() }
    }

    fun searchByTitle(query: String?): List<ExampleSearchResultDTO> {
        if (query.isNullOrBlank()) {
            return exampleRepository.findAll().map { it.toExampleSearchResultDTO() }
        }

        return exampleRepository.searchByTitle(query).map { it.toExampleSearchResultDTO() }
    }

    private fun Example.toExampleSearchResultDTO(): ExampleSearchResultDTO =
        ExampleSearchResultDTO(title, library, contents.map { it.language })

    fun findByTitle(title: String): ExampleDTO? {
        return exampleRepository.findByTitle(title)?.toDTO()
    }

    fun findLanguagesByTitle(title: String): List<String> {
        return exampleRepository.findLanguagesByTitle(title)
    }
}