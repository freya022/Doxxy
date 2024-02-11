package io.github.freya022.backend.provider

import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JsonProvider {
    @Bean
    fun json(): Json = Json.Default
}