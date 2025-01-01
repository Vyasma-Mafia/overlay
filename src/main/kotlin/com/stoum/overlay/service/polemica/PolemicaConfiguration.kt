package com.stoum.overlay.service.polemica

import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.github.mafia.vyasma.polemica.library.client.PolemicaClientImpl
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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
    fun polemicaWebClient(): PolemicaClient {
        return PolemicaClientImpl(
            polemicaBaseUrl,
            polemicaUsername,
            polemicaPassword
        )
    }
}
