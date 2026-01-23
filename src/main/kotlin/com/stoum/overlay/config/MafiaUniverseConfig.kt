package com.stoum.overlay.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "app.mafiauniverse", ignoreUnknownFields = true)
data class MafiaUniverseConfig(
    val enable: Boolean,
    val baseUrl: String,
    val crawlScheduler: Scheduler
) {
    @Bean
    fun mafiaUniverseCrawlScheduler() = crawlScheduler

    data class Scheduler(val enable: Boolean, val interval: Duration)
}
