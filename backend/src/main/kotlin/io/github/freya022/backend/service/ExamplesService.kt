package io.github.freya022.backend.service

import io.github.freya022.backend.entity.Example
import io.github.freya022.backend.entity.ExampleTarget
import io.github.freya022.backend.repository.ExampleRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.client.bodyWithType

private typealias SimpleClassName = String
private typealias QualifiedPartialIdentifier = String
private typealias FullQualifiedIdentifier = String

//TODO use common module with main bot
private enum class DocSourceType {
    JDA,
    BOT_COMMANDS,
    JAVA
}

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

        @Serializable
        data class RequestedTargets(
            val sourceTypeToSimpleClassNames: Map<DocSourceType, List<SimpleClassName>>,
            val sourceTypeToQualifiedPartialIdentifiers: Map<DocSourceType, List<QualifiedPartialIdentifier>>
        )

        @Serializable
        data class MappedTargets(
            val sourceTypeToSimpleClassNames: Map<DocSourceType, List<SimpleClassName>>,
            val sourceTypeToMappedIdentifiers: Map<DocSourceType, Map<QualifiedPartialIdentifier, List<FullQualifiedIdentifier>>>
        )

        val examples: List<IndexedExample> = docExampleClient.get()
            .uri("/index.json")
            .retrieve()
            .body<String>()!!
            .let(json::decodeFromString)

        // TODO this was not needed, all you could do is check whether the targets are correct, no need to map them
        //  When an example gets requested by its full identifier,
        //    the backend db checks for the full identifier *and* the partial identifier
        //  That way also allows linkage with recently-added overloads
        // Gets actual targets from the bot
        val requestedTargets = examples.groupBy { it.sourceType }.let { examplesBySourceType ->
            RequestedTargets(
                examplesBySourceType.mapValues { (_, examples) -> examples.flatMap { it.targets }.filterNot { '#' in it } },
                examplesBySourceType.mapValues { (_, examples) -> examples.flatMap { it.targets }.filter { '#' in it } }
            )
        }
        val mappedTargets: MappedTargets = botClient.post()
            .uri("/examples/targets")
            .bodyWithType(requestedTargets)
            .retrieve()
            .body<String>()!!
            .let(json::decodeFromString)

        exampleRepository.saveAllAndFlush(buildList {
            examples.forEach { dto ->
                dto.languages.forEach { language ->
                    val content = getExampleContent(dto.library, dto.name, language)
                    val targetEntities = dto.targets.flatMap { mappedTargets.sourceTypeToMappedIdentifiers[dto.sourceType]!![it]!! /*💀*/  }.map { ExampleTarget(it) }

                    this += Example(language, dto.title, content, targetEntities)
                }
            }
        })
    }

    private fun getExampleContent(library: String, name: String, language: String): String = client.get()
        .uri("https://raw.githubusercontent.com/freya022/doc-examples/master/examples/{library}/{name}/{language}.md", library, name, language)
        .retrieve()
        .body()!!
}