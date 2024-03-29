package io.github.freya022.backend.provider

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestClientProvider {
    @Bean
    fun botClient(@Value("\${bot.http.url}") botHttpUrl: String): RestClient =
        RestClient.create(botHttpUrl)
}