package com.stoum.overlay.service.polemica

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    value = ["app.polemicaEnable", "app.crawlScheduler.enable"],
    havingValue = "true"
)
@EnableScheduling
class PolemicaCrawlSchedulerComponent(val polemicaService: PolemicaService) {
    private val logger = LoggerFactory.getLogger(PolemicaCrawlSchedulerComponent::class.java.name)

    @Scheduled(fixedDelayString = "#{@crawlScheduler.interval.toMillis()}")
    private fun update() {
        try {
            logger.info("Game updates crawl started")
            polemicaService.crawl()
        } catch (e: Exception) {
            logger.error("Error on crawling: " + e.message, e)
        }
    }
}
