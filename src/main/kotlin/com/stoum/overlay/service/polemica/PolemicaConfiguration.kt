package com.stoum.overlay.service.polemica

import com.github.mafia.vyasma.polemicaachivementservice.crawler.PolemicaClient
import com.github.mafia.vyasma.polemicaachivementservice.crawler.PolemicaClientImpl
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@ConditionalOnProperty(value = ["app.polemicaEnable"], havingValue = "true")
class PolemicaConfiguration {
    @Value("\${polemica.api.baseUrl:https://app.polemicagame.com}")
    private lateinit var botBaseUrl: String

    @Value("\${POLEMICA_TOKEN}")
    private lateinit var botToken: String

    @Bean
    fun polemicaWebClient(): PolemicaClient {
        return PolemicaClientImpl(
            WebClient.builder()
                .baseUrl(botBaseUrl)
                .defaultHeader("Authorization", "Bearer $botToken")
                .build()
        )
    }
}
