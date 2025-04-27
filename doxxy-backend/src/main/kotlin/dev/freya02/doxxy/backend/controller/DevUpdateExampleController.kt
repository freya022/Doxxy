package dev.freya02.doxxy.backend.controller

import dev.freya02.doxxy.backend.service.ExamplesService
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

//@Profile("dev") //TODO enable once updates are using webhooks
@RestController
class DevUpdateExampleController(
    private val examplesService: ExamplesService
) {
    @PutMapping("/examples/update")
    fun updateExamples(
        @RequestParam ownerName: String,
        @RequestParam repoName: String,
        @RequestParam branchName: String
    ) = examplesService.updateExamples(ownerName, repoName, branchName)
}