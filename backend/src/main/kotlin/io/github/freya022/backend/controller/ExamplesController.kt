package io.github.freya022.backend.controller

import io.github.freya022.backend.dto.ExampleDTO
import io.github.freya022.backend.repository.dto.ExampleSearchResultDTO
import io.github.freya022.backend.service.ExamplesService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ExamplesController(
    private val examplesService: ExamplesService
) {
    @GetMapping("/examples")
    fun findByTarget(@RequestParam target: String): List<ExampleDTO> = examplesService.findByTarget(target)

    @GetMapping("/examples/search")
    fun searchByTitle(@RequestParam query: String?): List<ExampleSearchResultDTO> = examplesService.searchByTitle(query)

    @GetMapping("/example")
    fun findByTitle(@RequestParam title: String): ExampleDTO? = examplesService.findByTitle(title)
}