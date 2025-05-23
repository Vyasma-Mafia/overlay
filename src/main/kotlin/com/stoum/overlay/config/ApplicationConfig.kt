package com.stoum.overlay.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.validation.annotation.Validated
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "app", ignoreUnknownFields = true)
data class ApplicationConfig(
    val crawlScheduler: Scheduler,
    val polemicaEnable: Boolean,
    val development: Boolean
) {
    @Bean
    fun crawlScheduler() = crawlScheduler

    @Bean
    fun polemicaEnable() = polemicaEnable

    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()

    data class Scheduler(val enable: Boolean, val interval: Duration) {
    }
}
