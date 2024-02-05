package io.github.freya022.backend.provider

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestClientProvider {
    @Bean("docExampleClient")
    fun docExampleClient(): RestClient =
        RestClient.create("https://raw.githubusercontent.com/freya022/doc-examples/master")
}