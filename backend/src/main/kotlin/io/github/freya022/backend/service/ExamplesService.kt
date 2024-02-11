package io.github.freya022.backend.service

import io.github.freya022.backend.dto.ExampleDTO
import io.github.freya022.backend.dto.MissingTargets
import io.github.freya022.backend.dto.RequestedTargets
import io.github.freya022.backend.entity.Example
import io.github.freya022.backend.entity.ExampleContent
import io.github.freya022.backend.entity.ExampleTarget
import io.github.freya022.backend.repository.ExampleRepository
import io.github.freya022.backend.repository.dto.ExampleSearchResultDTO
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.client.bodyWithType

typealias SimpleClassName = String
typealias QualifiedPartialIdentifier = String

//TODO use common module with main bot
enum class DocSourceType {
    JDA,
    BOT_COMMANDS,
    JAVA
}

private val logger = KotlinLogging.logger { }

//TODO when an example gets requested by its full identifier,
//   check for the full identifier *and* the partial identifier
// That way also allows linkage with recently-added overloads
@Service
class ExamplesService(
    private val docExampleClient: RestClient,
    private val botClient: RestClient,
    private val exampleRepository: ExampleRepository,
    private val json: Json
) {
    private val client = RestClient.create()

    @Transactional
    fun updateExamples() {
        exampleRepository.removeAll()

        @Serializable
        data class IndexedExample(
            val name: String,
            val languages: List<String>,
            val library: String,
            val title: String,
            val targets: List<String>
        ) {
            val sourceType = when (library) {
                "JDA" -> DocSourceType.JDA
                "JDK" -> DocSourceType.JAVA
                "BotCommands" -> DocSourceType.BOT_COMMANDS
                else -> throw IllegalArgumentException("Unknown library: $library")
            }
        }

        val examples: List<IndexedExample> = docExampleClient.get()
            .uri("/index.json")
            .retrieve()
            .body<String>()!!
            .let(json::decodeFromString)

        // Gets actual targets from the bot
        val requestedTargets = examples.groupBy { it.sourceType }.let { examplesBySourceType ->
            RequestedTargets(
                examplesBySourceType.mapValues { (_, examples) -> examples.flatMap { it.targets }.filterNot { '#' in it } },
                examplesBySourceType.mapValues { (_, examples) -> examples.flatMap { it.targets }.filter { '#' in it } }
            )
        }
        val missingTargets: MissingTargets = botClient.post()
            .uri("/examples/targets/check")
            .bodyWithType(requestedTargets)
            .retrieve()
            .body<String>()!!
            .let(json::decodeFromString)

        missingTargets.sourceTypeToSimpleClassNames.forEach { (sourceType, simpleClassNames) ->
            if (simpleClassNames.isNotEmpty()) {
                logger.error { "Missing classes in $sourceType:\n${simpleClassNames.joinToString()}" }
            }
        }
        missingTargets.sourceTypeToPartialIdentifiers.forEach { (sourceType, partialIdentifiers) ->
            if (partialIdentifiers.isNotEmpty()) {
                logger.error { "Missing (possibly partial) identifiers in $sourceType:\n${partialIdentifiers.joinToString()}" }
            }
        }

        exampleRepository.saveAllAndFlush(examples.map { dto ->
            val contentEntities = dto.languages.map { language ->
                ExampleContent(language, getExampleContent(dto.library, dto.name, language))
            }
            val resolvableTargetEntities = dto.targets.filter {
                if ('#' !in it) {
                    // Filter out if class is missing
                    it !in missingTargets.sourceTypeToSimpleClassNames[dto.sourceType]!!
                } else {
                    // Filter out if partial identifier is missing
                    it !in missingTargets.sourceTypeToPartialIdentifiers[dto.sourceType]!!
                }
            }.map { ExampleTarget(it) }

            Example(dto.title, dto.library, contentEntities, resolvableTargetEntities)
        })
    }

    private fun getExampleContent(library: String, name: String, language: String): String = client.get()
        .uri("https://raw.githubusercontent.com/freya022/doc-examples/master/examples/{library}/{name}/{language}.md", library, name, language)
        .retrieve()
        .body()!!

    fun findByTarget(target: String): List<ExampleSearchResultDTO> {
        return exampleRepository.findByTarget(target)
    }

    fun searchByTitle(query: String?): List<ExampleSearchResultDTO> {
        if (query.isNullOrBlank()) {
            return exampleRepository.findAllAsSearchResults()
        }

        return exampleRepository.searchByTitle(query)
    }

    fun findByTitle(title: String): ExampleDTO? {
        return exampleRepository.findByTitle(title)?.let(::ExampleDTO)
    }
}