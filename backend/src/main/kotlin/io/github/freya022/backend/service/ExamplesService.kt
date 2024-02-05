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

@Service
class ExamplesService(
    private val docExampleClient: RestClient,
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
        )

        val examples: List<IndexedExample> = docExampleClient.get()
            .uri("/index.json")
            .retrieve()
            .body<String>()!!
            .let(json::decodeFromString)

        // Gets actual targets from the bot
        val allTargets = examples.flatMapTo(hashSetOf()) { it.targets }
        val targetMap: Map<String, List<String>> = allTargets.associateWith { listOf("$it()") } //TODO

        exampleRepository.saveAllAndFlush(buildList {
            examples.forEach { dto ->
                dto.languages.forEach { language ->
                    val content = getExampleContent(dto.library, dto.name, language)
                    val targetEntities = dto.targets.flatMap { targetMap[it]!! }.map { ExampleTarget(it) }

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