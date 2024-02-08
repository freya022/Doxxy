package io.github.freya022.backend.resource

import io.github.freya022.backend.dto.ExampleDTO
import io.github.freya022.backend.service.ExamplesService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ExamplesResource(
    private val examplesService: ExamplesService
) {
    @GetMapping("/examples")
    fun findBySignature(@RequestParam signature: String): List<ExampleDTO> = examplesService.findBySignature(signature)

    @GetMapping("/examples/search")
    fun searchByName(@RequestParam query: String): List<ExampleDTO> = examplesService.searchByTitle(query)
}