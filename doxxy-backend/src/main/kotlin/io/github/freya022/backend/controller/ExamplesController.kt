package io.github.freya022.backend.controller

import io.github.freya022.backend.service.ExamplesService
import io.github.freya022.doxxy.common.dto.ExampleDTO
import io.github.freya022.doxxy.common.dto.ExampleSearchResultDTO
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ExamplesController(
    private val examplesService: ExamplesService
) {
    @GetMapping("/examples")
    fun findByTarget(@RequestParam target: String): List<ExampleSearchResultDTO> = examplesService.findByTarget(target)

    @GetMapping("/examples/search")
    fun searchByTitle(@RequestParam query: String?): List<ExampleSearchResultDTO> = examplesService.searchByTitle(query)

    @GetMapping("/example")
    fun findByTitle(@RequestParam title: String): ResponseEntity<ExampleDTO> {
        return examplesService.findByTitle(title)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/example/languages")
    fun findLanguagesByTitle(@RequestParam title: String): List<String> = examplesService.findLanguagesByTitle(title)
}