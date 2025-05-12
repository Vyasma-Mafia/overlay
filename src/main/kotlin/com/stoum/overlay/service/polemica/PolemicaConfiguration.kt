package com.stoum.overlay.service.polemica

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mafia.vyasma.polemica.library.client.GamePointsService
import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.github.mafia.vyasma.polemica.library.client.PolemicaClientImpl
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
@ConditionalOnProperty(value = ["app.polemicaEnable"], havingValue = "true")
class PolemicaConfiguration {
    @Value("\${polemica.api.baseUrl:https://app.polemicagame.com}")
    private lateinit var polemicaBaseUrl: String

    @Value("\${POLEMICA_USERNAME}")
    private lateinit var polemicaUsername: String

    @Value("\${POLEMICA_PASSWORD}")
    private lateinit var polemicaPassword: String

    @Bean
    fun polemicaWebClient(
        objectMapper: ObjectMapper
    ): PolemicaClient {
        return PolemicaClientImpl(
            polemicaBaseUrl,
            polemicaUsername,
            polemicaPassword,
            objectMapper
        )
    }

    @Bean
    fun polemicaPointsService(restTemplate: RestTemplate): GamePointsService {
        return GamePointsService(restTemplate)
    }
}
